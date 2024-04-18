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
package com.reid.pdfbatchsplitter.domain;

import com.reid.pdfbatchsplitter.PDFBatchSplitter;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 *
 * @author pmreid
 *
 * A domain class to hold the core components of a document page
 */
public class ComponentPage {

    private String additionalIdentifiers;
    private String identifier;
    private PDDocument pdfPage;
    private int pageNumber; // page number in original document, starts at 0
    private String pageContents;
    private boolean interpreted; // set to true after the page has been interpreted and identifiers extracted

    public ComponentPage(PDDocument p, String c, int n) {
        this.pdfPage = p;
        this.pageContents = c;
        this.pageNumber = n;
        this.additionalIdentifiers = "";
    }

    public String getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    public void setAdditionalIdentifiers(String additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void appendIdentifier(String i) {
        if (this.additionalIdentifiers.equals("")) {
            this.additionalIdentifiers = i;
        } else {
            this.additionalIdentifiers = this.additionalIdentifiers + PDFBatchSplitter.DEFAULT_SEPARATOR + i;
        }
    }

    public PDDocument getPdfPage() {
        return pdfPage;
    }

    public String getPageContents() {
        return pageContents;
    }

    public void setPageContents(String pageContents) {
        this.pageContents = pageContents;
    }

    public boolean isInterpreted() {
        return interpreted;
    }

    public void setInterpreted(boolean interpreted) {
        this.interpreted = interpreted;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getCompleteIdentifier() {
        if (this.additionalIdentifiers.length() > 0) {
            return (this.identifier + PDFBatchSplitter.DEFAULT_SEPARATOR + this.additionalIdentifiers).replaceAll(" ", "");
        } else {
            return this.identifier;
        }
    }

}
