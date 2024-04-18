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
package com.reid.pdfbatchsplitter.domain.primitives;

import java.util.regex.Pattern;

/**
 * A primitive class to hold the search terms and the nature of the response
 * expected
 *
 * @author pmreid
 */
public class SearchTerm {

    public static final int TYPE_NUMERICAL = 1;
    public static final int TYPE_ALPHANUMERICAL = 2;
    public static final int TYPE_ALPHA = 3;

    private int type;
    private String label;
    private String regex;
    private int outputGroup;

    public SearchTerm(int t, String p, String r, int o) {
        this.type = t;
        this.label = p;
        this.regex = r;
        this.outputGroup = o;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String l) {
        this.label = l;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public int getOutputGroup() {
        return outputGroup;
    }

    public void setOutputGroup(int outputGroup) {
        this.outputGroup = outputGroup;
    }

}
