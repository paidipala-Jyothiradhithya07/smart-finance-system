package com.smartfinance.statement.dedup;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.statement.model.ParsedTransaction;

class FingerprintsTest {

    private static ParsedTransaction tx(int row, String date, String description, String amount,
                                        TransactionType type) {
        return new ParsedTransaction(row, LocalDate.parse(date), description, new BigDecimal(amount),
                type, null, null);
    }

    @Test
    void fileHash_isStableAndSensitiveToContent() {
        String hash = Fingerprints.fileHash("hello".getBytes(StandardCharsets.UTF_8));

        assertThat(hash).hasSize(64);
        assertThat(hash).isEqualTo(Fingerprints.fileHash("hello".getBytes(StandardCharsets.UTF_8)));
        assertThat(hash).isNotEqualTo(Fingerprints.fileHash("hellp".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void identicalRowsInOneFile_getDifferentFingerprints() {
        List<String> hashes = Fingerprints.transactionHashes(List.of(
                tx(2, "2026-05-02", "CHAI POINT", "50.00", TransactionType.DEBIT),
                tx(3, "2026-05-02", "CHAI POINT", "50.00", TransactionType.DEBIT)));

        assertThat(hashes).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void sameTransactionsInAnotherFile_getTheSameFingerprints() {
        List<String> january = Fingerprints.transactionHashes(List.of(
                tx(2, "2026-01-01", "SALARY CREDIT", "50000.00", TransactionType.CREDIT),
                tx(3, "2026-01-03", "RENT PAYMENT", "15000.00", TransactionType.DEBIT)));

        List<String> overlapping = Fingerprints.transactionHashes(List.of(
                tx(2, "2025-12-31", "UBER", "1200.00", TransactionType.DEBIT),
                tx(3, "2026-01-01", "SALARY CREDIT", "50000", TransactionType.CREDIT),
                tx(4, "2026-01-03", "RENT PAYMENT", "15000.00", TransactionType.DEBIT)));

        assertThat(overlapping.get(1)).isEqualTo(january.get(0));
        assertThat(overlapping.get(2)).isEqualTo(january.get(1));
    }

    @Test
    void caseAndPunctuationInTheDescriptionDoNotMatter() {
        String a = Fingerprints.transactionHashes(List.of(
                tx(2, "2026-01-05", "Swiggy  Order.", "850.00", TransactionType.DEBIT))).get(0);
        String b = Fingerprints.transactionHashes(List.of(
                tx(9, "2026-01-05", "SWIGGY ORDER", "850.00", TransactionType.DEBIT))).get(0);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void aDifferentDateAmountOrType_changesTheFingerprint() {
        String base = Fingerprints.transactionHashes(List.of(
                tx(2, "2026-01-05", "SWIGGY", "850.00", TransactionType.DEBIT))).get(0);

        assertThat(Fingerprints.transactionHashes(List.of(
                tx(2, "2026-01-06", "SWIGGY", "850.00", TransactionType.DEBIT))).get(0)).isNotEqualTo(base);
        assertThat(Fingerprints.transactionHashes(List.of(
                tx(2, "2026-01-05", "SWIGGY", "851.00", TransactionType.DEBIT))).get(0)).isNotEqualTo(base);
        assertThat(Fingerprints.transactionHashes(List.of(
                tx(2, "2026-01-05", "SWIGGY", "850.00", TransactionType.CREDIT))).get(0)).isNotEqualTo(base);
    }
}