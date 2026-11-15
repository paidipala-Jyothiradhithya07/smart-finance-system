package com.smartfinance.statement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.statement.model.ParsedTransaction;

class ChronologicalOrderTest {

    private static ParsedTransaction tx(String date, String description) {
        return new ParsedTransaction(1, LocalDate.parse(date), description, new BigDecimal("10.00"),
                TransactionType.DEBIT, null, null);
    }

    @Test
    void oldestFirstFilesKeepTheirOrder() {
        List<ParsedTransaction> sorted = ChronologicalOrder.sort(List.of(
                tx("2026-07-01", "A"), tx("2026-07-02", "B1"), tx("2026-07-02", "B2"), tx("2026-07-03", "C")));

        assertThat(sorted).extracting(ParsedTransaction::description).containsExactly("A", "B1", "B2", "C");
    }

    @Test
    void newestFirstFilesAreReversed_andSameDayRowsKeepTheirRealOrder() {
        // In a newest-first file the later payment of a day is listed first
        List<ParsedTransaction> sorted = ChronologicalOrder.sort(List.of(
                tx("2026-07-03", "C"), tx("2026-07-02", "B2"), tx("2026-07-02", "B1"), tx("2026-07-01", "A")));

        assertThat(sorted).extracting(ParsedTransaction::description).containsExactly("A", "B1", "B2", "C");
    }

    @Test
    void aMixedFileEndsUpSortedByDate() {
        List<ParsedTransaction> sorted = ChronologicalOrder.sort(List.of(
                tx("2026-07-02", "B"), tx("2026-07-01", "A"), tx("2026-07-05", "D"), tx("2026-07-03", "C")));

        assertThat(sorted).extracting(ParsedTransaction::description).containsExactly("A", "B", "C", "D");
    }

    @Test
    void emptyAndSingleRowListsAreFine() {
        assertThat(ChronologicalOrder.sort(List.of())).isEmpty();
        assertThat(ChronologicalOrder.sort(List.of(tx("2026-07-01", "A")))).hasSize(1);
    }
}