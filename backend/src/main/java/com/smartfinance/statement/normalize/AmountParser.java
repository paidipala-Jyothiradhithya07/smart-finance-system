package com.smartfinance.statement.normalize;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmountParser {
	public enum Hint { DEBIT, CREDIT }

    /** The signed number as written, plus a Dr/Cr marker if the text had one. */
    public record ParsedAmount(BigDecimal value, Hint hint) {
    }

    private static final Set<String> BLANK_MARKERS = Set.of("-", "--", "nil", "n/a", "na", "null");
    private static final Pattern DR_CR_SUFFIX = Pattern.compile("(?i)^(.*?)\\s*(dr|cr)\\.?$");
    private static final Pattern DR_CR_PREFIX = Pattern.compile("(?i)^(dr|cr)\\.?\\s*(.*)$");
    // \u20B9 is the rupee sign and \u00A3 is the pound sign (escapes keep this file safe in any editor encoding)
    private static final Pattern CURRENCY = Pattern.compile("(?i)(inr|rs\\.?|usd|gbp|[\u20B9$\u00A3])");
    private static final Pattern NUMBER = Pattern.compile("^(\\d+(\\.\\d*)?|\\.\\d+)$");

    private AmountParser() {
    }

    /** True for empty text and for placeholders banks use in empty cells, such as "-" or "N/A". */
    public static boolean isBlank(String raw) {
        if (raw == null) {
            return true;
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        return text.isEmpty() || BLANK_MARKERS.contains(text);
    }

    /**
     * Understands: 1,200.50   (500)   -500   500-   +500   500 Dr   1,200.00Cr   Cr 750   Rs. 1,200   INR 99.5
     * Returns empty for blank cells. Throws IllegalArgumentException when the text is not an amount.
     */
    public static Optional<ParsedAmount> parse(String raw) {
        if (isBlank(raw)) {
            return Optional.empty();
        }
        String text = raw.trim();
        Hint hint = null;

        Matcher suffix = DR_CR_SUFFIX.matcher(text);
        if (suffix.matches()) {
            hint = hintOf(suffix.group(2));
            text = suffix.group(1);
        } else {
            Matcher prefix = DR_CR_PREFIX.matcher(text);
            if (prefix.matches()) {
                hint = hintOf(prefix.group(1));
                text = prefix.group(2);
            }
        }

        text = CURRENCY.matcher(text).replaceAll("");
        text = text.replace(",", "").replaceAll("\\s+", "");

        boolean negative = false;
        if (text.length() > 2 && text.startsWith("(") && text.endsWith(")")) {
            negative = true;
            text = text.substring(1, text.length() - 1);
        }
        if (text.startsWith("-")) {
            negative = true;
            text = text.substring(1);
        } else if (text.startsWith("+")) {
            text = text.substring(1);
        }
        if (text.endsWith("-")) {
            negative = true;
            text = text.substring(0, text.length() - 1);
        }

        if (!NUMBER.matcher(text).matches()) {
            throw new IllegalArgumentException("Not a valid amount");
        }
        BigDecimal value = new BigDecimal(text);
        return Optional.of(new ParsedAmount(negative ? value.negate() : value, hint));
    }

    private static Hint hintOf(String marker) {
        return marker.equalsIgnoreCase("dr") ? Hint.DEBIT : Hint.CREDIT;
    }
}
