package com.smartfinance.statement;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern NOT_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]+");

    private TextNormalizer() {
    }

    /** "Swiggy  Order." and "SWIGGY ORDER" both become "swiggy order". Works for any language. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return NOT_LETTER_OR_DIGIT.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }
}