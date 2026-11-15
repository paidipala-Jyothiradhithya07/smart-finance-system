package com.smartfinance.statement.dedup;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import com.smartfinance.statement.TextNormalizer;
import com.smartfinance.statement.model.ParsedTransaction;

public final class Fingerprints {

    private Fingerprints() {
    }

    /** SHA-256 of the file bytes, as 64 hex characters. */
    public static String fileHash(byte[] content) {
        return sha256Hex(content);
    }

    /**
     * One fingerprint per transaction, in the same order as the input.
     * It is built from date, description, amount and type, plus an occurrence number: the first identical row
     * gets 1, the second gets 2, and so on. So two real payments of the same amount on the same day stay
     * different, while the same payment in an overlapping statement gets the same fingerprint again.
     * The balance and reference are left out on purpose, so a statement with those columns still matches
     * one without them.
     */
    public static List<String> transactionHashes(List<ParsedTransaction> transactions) {
        Map<String, Integer> occurrences = new HashMap<>();
        List<String> hashes = new ArrayList<>(transactions.size());
        for (ParsedTransaction t : transactions) {
            String key = t.date()
                    + "|" + TextNormalizer.normalize(t.description())
                    + "|" + t.amount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + "|" + t.type().name();
            int occurrence = occurrences.merge(key, 1, Integer::sum);
            hashes.add(sha256Hex((key + "|" + occurrence).getBytes(StandardCharsets.UTF_8)));
        }
        return hashes;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}