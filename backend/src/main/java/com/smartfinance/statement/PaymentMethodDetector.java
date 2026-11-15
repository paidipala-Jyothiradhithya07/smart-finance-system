package com.smartfinance.statement;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentMethodDetector {

    // The keyword must not touch other letters: "UPI2026" matches, "CARDIOLOGY" and "POSTPAID" do not
    private static final Pattern PATTERN = Pattern.compile(
            "(?<![a-z])(upi|neft|imps|rtgs|atm|nach|ecs|cheque|chq|card|pos)(?![a-z])");

    private PaymentMethodDetector() {
    }

    /** Looks in the description and the reference. Returns null when nothing is recognised. */
    public static String detect(String description, String reference) {
        String text = ((description == null ? "" : description) + " " + (reference == null ? "" : reference))
                .toLowerCase(Locale.ROOT);
        Matcher matcher = PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return switch (matcher.group(1)) {
            case "upi" -> "UPI";
            case "neft" -> "NEFT";
            case "imps" -> "IMPS";
            case "rtgs" -> "RTGS";
            case "atm" -> "ATM";
            case "nach", "ecs" -> "NACH";
            case "cheque", "chq" -> "CHEQUE";
            default -> "CARD";
        };
    }
}