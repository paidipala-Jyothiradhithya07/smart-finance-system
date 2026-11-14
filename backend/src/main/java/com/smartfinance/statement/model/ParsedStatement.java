package com.smartfinance.statement.model;


import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.smartfinance.statement.FileType;
import com.smartfinance.statement.mapping.ColumnMapping;
import com.smartfinance.statement.normalize.DateOrder;

public record ParsedStatement(
        String fileName,
        FileType fileType,
        int headerRowNumber,
        ColumnMapping mapping,
        DateOrder dateOrder,
        int totalDataRows,
        int skippedRows,
        int invalidRows,
        List<ParsedTransaction> transactions,
        List<RowError> errors,
        List<String> warnings) {

    public LocalDate periodStart() {
        return transactions.stream().map(ParsedTransaction::date).min(Comparator.naturalOrder()).orElse(null);
    }

    public LocalDate periodEnd() {
        return transactions.stream().map(ParsedTransaction::date).max(Comparator.naturalOrder()).orElse(null);
    }

    /** Number of valid transactions in each calendar month, oldest first. */
    public SortedMap<YearMonth, Long> monthCounts() {
        return transactions.stream().collect(Collectors.groupingBy(
                t -> YearMonth.from(t.date()), TreeMap::new, Collectors.counting()));
    }
}
