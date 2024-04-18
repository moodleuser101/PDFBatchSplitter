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
    private static JFrame processingFrame;
    private static List<SearchTerm> search;

    /**
     * main method of the application
     *
     * @param args command-line parameters passed to the application; assumed to
     * be none
     */
    public static void main(String[] args) {
        initializeSettings(); // initialize settings like UI preferences etc.
        initProcessingWindow();

        /*
        Select locations, initialize search terms and do preparatory work:
         */
        File src = PDFBatchSplitter.fileBrowse("Select Source PDF", FileDialog.LOAD, ".pdf"); // show dialog to select source file
        if (src == null) {
            PDFBatchSplitter.outputExceptionToUser(new Exception("Cancel was clicked on source file selection; exiting..."));
            System.exit(0);
        }

        File dest = PDFBatchSplitter.folderBrowse(); // show dialog to select destination
        if (dest == null) {
            PDFBatchSplitter.triggerExit("Destination folder was not selected properly; exiting.");
        }

        String prefix = JOptionPane.showInputDialog("Input the prefix to apply to newly created files", "ExamTimetable"); // all created PDF files start with filename with this prefix
        if (prefix == null) {
            PDFBatchSplitter.triggerExit("Cancel was clicked on prefix selection; exiting...");
        }
        String suffix = "pdf";
        processingFrame.setVisible(true);
        addSearchTerms();

        /*
        Meat of the processing starts from here:
         */
        try {
            PDFSplitter splitter = new PDFSplitter(src, dest, search, prefix, suffix, false);
            splitter.readPDF(); // read PDFs into variables
            splitter.interpretPDFPages(); // extract identifiers

            /*
            We then initiate the batch writing process:
             */
            if (splitter.writeBatch()) {
                // success
                processingFrame.dispose();
                PDFBatchSplitter.outputMessageToUser("Wrote " + splitter.getPageCount() + " PDF files to destination: " + splitter.getDestinationAsString());
                System.exit(0);
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

    public static void outputExceptionToUser(Exception ex) {
        JOptionPane.showMessageDialog(null, "An error occurred: \n\n" + ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

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

    private static File folderBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.home"))); // start at application current directory
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else {
            return null;
        }
    }

    private static void triggerExit(String msg) {
        PDFBatchSplitter.outputExceptionToUser(new Exception(msg));
        System.exit(0);
    }

    private static void initializeSettings() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            PDFBatchSplitter.outputExceptionToUser(ex);
        }
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }

    /**
     * Initiates the List object to store the SearchTerms, and populates with
     * sample uses
     */
    private static void addSearchTerms() {
        search = new ArrayList<>();
        search.add(new SearchTerm(SearchTerm.TYPE_NUMERICAL, "Admission Number", "([0-9]{5,6}).*(Admission Number)", 1));
        search.add(new SearchTerm(SearchTerm.TYPE_NUMERICAL, "Candidate Number", "([0-9]{4})(Candidate Number)", 1));
        search.add(new SearchTerm(SearchTerm.TYPE_ALPHANUMERICAL, "UPN", "(UPN:?)[\\s\\S]:?([a-zA-Z0-9]+)", 2));
        search.add(new SearchTerm(SearchTerm.TYPE_NUMERICAL, "ULN", "(ULN:?)[\\s\\S]:?([0-9]+)", 2));
        search.add(new SearchTerm(SearchTerm.TYPE_NUMERICAL, "Candidate Number", "([0-9]{4})\\s[\\s\\S]*[0-9]{10}[a-zA-Z][\\s\\S]*(Candidate Number)", 1));
        search.add(new SearchTerm(SearchTerm.TYPE_NUMERICAL, "ULN", "([0-9]{10})(ULN)", 1));
        search.add(new SearchTerm(SearchTerm.TYPE_ALPHANUMERICAL, "Name", "([a-zA-Z]+,\\s[a-zA-Z]+)(Name)", 1));
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
        ImageIcon loading = new ImageIcon(PDFBatchSplitter.class.getResource("/com/reid/pdfbatchsplitter/res/loading.gif"));
        // loading image courtesy of https://tenor.com/en-GB/view/loading-gif-26545612
        processingFrame.add(new JLabel(" processing...", loading, JLabel.CENTER));
        /*
        Hat-tip https://stackoverflow.com/questions/7634402/creating-a-nice-loading-animation
         */
    }
}
