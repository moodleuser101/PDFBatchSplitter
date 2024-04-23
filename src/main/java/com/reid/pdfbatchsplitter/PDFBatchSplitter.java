package com.reid.pdfbatchsplitter;

import com.reid.pdfbatchsplitter.domain.primitives.SearchTerm;
import com.reid.pdfbatchsplitter.service.PDFSplitter;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author pmreid
 */
public class PDFBatchSplitter {

    public static final String DEFAULT_SEPARATOR = "_";
    public static MainWindow mw;
    private static JFrame processingFrame;
    public static List<SearchTerm> search;
    private static File sourceFile;
    private static File destinationFile;
    private static String prefix;
    private static String suffix = "pdf";

    /**
     * main method of the application
     *
     * @param args command-line parameters passed to the application; assumed to
     * be none
     */
    public static void main(String[] args) {
        initializeSettings(); // initialize settings like UI preferences etc.
        // multiple threads will be needed to split the GUI from the actual splitter, else GUI lag results:
        Thread pFrame = new Thread(new Runnable() {
            @Override
            public void run() {
                initProcessingWindow();
            }
        });
        pFrame.start();

        addSearchTerms();
        mw = new MainWindow();
        mw.setVisible(true);
    }

    /**
     * Initiates the List object to store the SearchTerms, and populates with
     * sample uses
     */
    private static void addSearchTerms() {
        search = new ArrayList<>();
        search.add(new SearchTerm("Admission Number", "([0-9]{5,6}).*(Admission Number)", 1));
        search.add(new SearchTerm("Candidate Number", "([0-9]{4})(Candidate Number)", 1));
        search.add(new SearchTerm("Candidate Number", "([0-9]{4})\\s[\\s\\S]*[0-9]{10}[a-zA-Z][\\s\\S]*(Candidate Number)", 1));
        search.add(new SearchTerm("UPN", "(UPN:?)[\\s\\S]:?([a-zA-Z0-9]+)", 2));
        //search.add(new SearchTerm("ULN", "(ULN:?)[\\s\\S]:?([0-9]+)", 2));
        //search.add(new SearchTerm("ULN", "([0-9]{10})(ULN)", 1));
        search.add(new SearchTerm("Name", "([a-zA-Z]+\\-?[a-zA-Z]+,\\s[a-zA-Z]+\\-?[a-zA-Z]+?)(Name)", 1));
    }

    /**
     * Takes input as the proposed prefix to append to destination files,
     * performs basic 'non-empty' checks on proposed parameters, and then tries
     * to initiate the splitting process
     *
     * @param prefix text to prepend to destination filenames.
     * @throws Exception User-friendly exception if basic checks on parameters
     * fail
     */
    public static void processBatch(String prefix) throws Exception {
        /*
        First check that the source, destination, prefix and search terms are valid
        The validity of the paths etc is checked in PDFSplitter, but we need to at least ensure they are non-null
         */
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new Exception("The supplied prefix was not valid");
        } else if (sourceFile == null || destinationFile == null) {
            throw new Exception("The source or destination locations are not set");
        } else if (search == null || search.isEmpty()) {
            throw new Exception("There were no search terms to find");
        }
        processingFrame.setVisible(true);
        // new Thread for the split process, to reduce GUI lag:
        Thread splitterThread = new Thread(new Runnable() {
            public void run() {
                PDFSplitter splitter;
                try {
                    splitter = new PDFSplitter(sourceFile, destinationFile, search, escapePrefix(prefix), suffix, true);
                    splitter.readPDF(); // read PDFs into variables
                    splitter.interpretPDFPages(); // extract identifiers

                    //We then initiate the batch writing process:
                    if (splitter.writeBatch()) {
                        // success
                        processingFrame.dispose();
                        PDFBatchSplitter.outputMessageToUser("Wrote " + splitter.getPageCount() + " PDF files to destination: " + splitter.getDestinationAsString());
                        mw.disableElements();
                    } else {
                        // fail
                        processingFrame.dispose();
                        PDFBatchSplitter.outputExceptionToUser(new Exception("Error writing PDF files to disk..."));
                    }
                } catch (IOException ex) {
                    PDFBatchSplitter.outputExceptionToUser(ex);
                    System.exit(1);
                }
            }
        });
        splitterThread.start(); // invokes execution of the splitter
    }

    /**
     * Simple helper method to escape white space characters from the supplied
     * prefix
     *
     * @param p User-entered prefix
     * @return Cleansed prefix
     */
    private static String escapePrefix(String p) {
        return p.replaceAll("[^A-Za-z0-9]", "");
    }

    /**
     * Helper method to select the source file to process
     *
     * @return String reference to the full path to the source file
     */
    public static String selectSourceFile() {
        sourceFile = PDFBatchSplitter.fileBrowse("Select Source PDF", FileDialog.LOAD, ".pdf"); // show dialog to select source file
        if (sourceFile == null) {
            PDFBatchSplitter.outputExceptionToUser(new Exception("Cancel was clicked on source file selection; it will not be possible to proceed until a source is selected..."));
            return null;
        } else {
            return sourceFile.getAbsolutePath();
        }
    }

    /**
     * Helper method to select the destination directory
     *
     * @return String reference to the full path of destination directory
     */
    public static String selectDestinationDirectory() {
        destinationFile = PDFBatchSplitter.folderBrowse(); // show dialog to select destination
        if (destinationFile == null) {
            PDFBatchSplitter.triggerExit("Destination folder was not selected properly; it will not be possible to proceed until it is set.");
            return null;
        } else {
            return destinationFile.getAbsolutePath();
        }
    }

    /**
     * Simple helper method to output an exception to the user in a pop-up
     * message box
     *
     * @param ex Populated Java exception
     */
    public static void outputExceptionToUser(Exception ex) {
        JOptionPane.showMessageDialog(null, "An error occurred: \n\n" + ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Simple helper method to output a message to the user in a pop-up message
     * box
     *
     * @param msg Textual representation of the message to display
     */
    public static void outputMessageToUser(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a "save as"/"open" file dialog
     *
     * @param dialogTitle Title of the dialog box
     * @param dialogType a FileDialog.{SAVE, LOAD} reference
     * @param fileFilter filename filter, eg ".csv" or ".sdc"; must have a dot
     * and no star
     * @return Java File reference
     */
    private static File fileBrowse(String dialogTitle, int dialogType, String fileFilter) {
        try {
            FileDialog fileDialog = new FileDialog(new Frame(), dialogTitle, dialogType);
            fileDialog.setDirectory(System.getProperty("user.home"));
            String fileFilter2;
            if (!fileFilter.contains(".")) {
                fileFilter2 = "." + fileFilter;
            } else {
                fileFilter2 = fileFilter;
            }
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(fileFilter2);
                }
            });
            fileDialog.setVisible(true);
            String fileName = fileDialog.getFile();
            if (fileName != null) {
                if (!fileName.endsWith(fileFilter)) {
                    fileName = fileName + fileFilter;
                }
                File file = new File(fileDialog.getDirectory(), fileName);
                return file;
            }
        } catch (Exception ex) {
            Exception ex2 = new Exception("Error processing file: \n" + ex.getLocalizedMessage());
            PDFBatchSplitter.outputExceptionToUser(ex2);
        }
        return null;
    }

    /**
     * Helper method to present a graphical file chooser window to select a
     * folder
     *
     * @return reference to the selected destination as a File
     */
    private static File folderBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.home"))); // start at user's home directory
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else {
            return null;
        }
    }

    /**
     * Helper method to display an error message to the user, and then exit
     * application.
     *
     * @param msg
     */
    private static void triggerExit(String msg) {
        PDFBatchSplitter.outputExceptionToUser(new Exception(msg));
        System.exit(0);
    }

    /**
     * Simple helper method to set up the UI look and feel so it's native to the
     * user's Operating System platform and avoids the Java Swing default
     * interface
     */
    private static void initializeSettings() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            PDFBatchSplitter.outputExceptionToUser(ex);
        }
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); // prevents superfluous logging by Apache PDFBox
    }

    /**
     * Displays a pop-up window to user to signify that batch job is running
     */
    private static void initProcessingWindow() {
        if (PDFBatchSplitter.processingFrame == null) {
            PDFBatchSplitter.processingFrame = new JFrame();
        }
        processingFrame.setAlwaysOnTop(true);
        processingFrame.setAutoRequestFocus(true);
        processingFrame.setResizable(false);
        processingFrame.setUndecorated(true);
        processingFrame.setSize(400, 300);
        processingFrame.setLocationRelativeTo(null);
        ImageIcon loading = new ImageIcon(PDFBatchSplitter.class
                .getResource("/com/reid/pdfbatchsplitter/res/loading.gif"));
        // loading image courtesy of https://tenor.com/en-GB/view/loading-gif-26545612
        processingFrame.add(new JLabel(" processing...", loading, JLabel.CENTER));
        /*
        Hat-tip https://stackoverflow.com/questions/7634402/creating-a-nice-loading-animation
         */
    }
}
