/*
 * Copyright (C) 2024 pmreid
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.reid.pdfbatchsplitter.service;

import com.reid.pdfbatchsplitter.PDFBatchSplitter;
import com.reid.pdfbatchsplitter.domain.ComponentPage;
import com.reid.pdfbatchsplitter.domain.primitives.SearchTerm;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Implementation of the splitter to take a PDF batch file and split into
 * component pages
 *
 * @author pmreid
 */
public class PDFSplitter {

    private File destination;
    private File source;
    private List<SearchTerm> searchTerms;
    private List<ComponentPage> pages;
    private String prefix;
    private String suffix;
    private PDDocument sourcePDF;
    private boolean showPDFAsText; // used for debugging;

    /**
     * Instantiates a new Splitter object with known Java File objects for the
     * source and destination
     *
     * @param s Populated source File
     * @param d Populated File object representing the destination folder for
     * extraction/dumping
     * @param t Populated List of search terms to extract to form the filenames
     * etc
     * @param p prefix for destination filenames
     * @param suff suffix for destination filenames
     * @param debug when true, one of the PDFs is outputted to the console for
     * analysis
     * @throws java.io.IOException
     */
    public PDFSplitter(File s, File d, List<SearchTerm> t, String p, String suff, boolean debug) throws IOException {
        if (!checkPaths(s, d)) {
            throw new IOException("The supplied paths for the source and destination were not valid!");
        }
        this.source = s;
        this.destination = d;
        this.searchTerms = t;
        pages = new ArrayList<>();
        this.prefix = p;
        this.suffix = suff;
        this.showPDFAsText = debug;

    }

    /**
     * Initiates
     *
     * @param s Full path to the source PDF file as a raw string
     * @param d Raw string of the full path to the destination directory
     * @param t Populated List of search terms to extract to form the filenames
     * etc.
     * @param p prefix for destination filenames
     * @param suff suffix for destination filenames
     * @throws java.io.IOException
     */
    public PDFSplitter(String s, String d, List<SearchTerm> t, String p, String suff, boolean debug) throws IOException {
        /*
        First, we test if s and d can be turned into accessible File paths....
         */
        this(new File(s), new File(d), t, p, suff, debug);
    }

    /**
     * Reads a source PDF and splits it into its constituent pages, creating a
     * List of ComponentPage objects
     *
     * @return true on success
     */
    public boolean readPDF() {
        boolean goodRead = true;
        try {
            sourcePDF = Loader.loadPDF(new RandomAccessReadBufferedFile(source));
            Splitter PDFSplitter = new Splitter();
            List<PDDocument> allPages = PDFSplitter.split(sourcePDF);
            PDFTextStripper reader = new PDFTextStripper();
            int pageNum = 0;
            int randomDisplayPage = 0;
            if (this.showPDFAsText) {
                // select a random page number as output:
                randomDisplayPage = ThreadLocalRandom.current().nextInt(0, allPages.size());
            }

            for (PDDocument thisPage : allPages) {
                ComponentPage p = new ComponentPage(thisPage, reader.getText(thisPage), pageNum);
                pages.add(p);

                if (this.showPDFAsText) {
                    if (pageNum == randomDisplayPage) {
                        PDFBatchSplitter.mw.updatePDFViewer(p.getPageContents());
                    }
                }
                pageNum++;
            }

        } catch (IOException ex) {
            System.out.println("Error loading PDF: " + ex.getLocalizedMessage());
            goodRead = false;
            try {
                sourcePDF.close();
            } catch (IOException ex1) {

            }
        }
        return goodRead;
    }

    /**
     * Cycles through the ComponentPages, and interprets any identifiers that it
     * can from the text using the List of SearchTerms, writing back to the
     * ComponentPages
     */
    public void interpretPDFPages() {
        for (ComponentPage page : pages) {
            boolean primaryIdentifier = true; // the first element of the SearchTerm list is the primary identifier to use; all subsequent elements are merely 'additional' identifiers
            for (SearchTerm search : searchTerms) {
                Pattern pat;
                pat = Pattern.compile(search.getRegex(), Pattern.DOTALL);
                Matcher matcher = pat.matcher(page.getPageContents());
                if (matcher.find()) {
                    String identifier = matcher.group(search.getOutputGroup()).trim();
                    page.setInterpreted(true);
                    if (primaryIdentifier) {
                        page.setIdentifier(identifier);
                    } else {
                        page.appendIdentifier(identifier);
                    }
                    primaryIdentifier = false;
                }
            }
        }
    }

    /**
     * Writes a batch of PDF files
     *
     * @return true on success
     * @throws java.io.IOException
     */
    public boolean writeBatch() throws IOException {
        boolean success = true;
        int numFailures = 0;
        for (ComponentPage page : pages) {
            String filename;
            if (page.isInterpreted()) {
                filename = this.prefix + PDFBatchSplitter.DEFAULT_SEPARATOR + page.getCompleteIdentifier() + "." + this.suffix;
            } else {
                numFailures++;
                filename = "AAA_FAILED_TO_READ_" + numFailures + "." + this.suffix;
            }
            File output = new File(this.destination.getAbsolutePath() + "/" + filename);
            page.getPdfPage().save(output);
            page.getPdfPage().close();
        }
        sourcePDF.close();
        return success;
    }

    /**
     * Helper method to check whether supplied file paths are valid
     *
     * @param s Proposed source file
     * @param d Proposed destination file
     */
    private boolean checkPaths(File s, File d) {
        boolean isValid = true;

        if (!(s.exists() && s.canRead() && s.isFile())) {
            isValid = false;
        }
        if (!(d.canWrite() && d.isDirectory())) {
            isValid = false;
        }
        return isValid;
    }

    public String getDestinationAsString() {
        return this.destination.getAbsolutePath();
    }

    public int getPageCount() {
        return this.pages.size();
    }
}
