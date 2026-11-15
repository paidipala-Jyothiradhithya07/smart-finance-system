package com.smartfinance.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ImportFileResultDTO(
        String fileName,
        ImportStatus status,
        String message,
        Long statementId,
        int transactionsDetected,
        int imported,
        int duplicates,
        int invalid,
        int skipped,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<MonthCountDTO> monthsDetected,
        List<String> warnings,
        List<RowErrorDTO> errors,
        boolean errorsTruncated,
        List<DuplicateRowDTO> duplicateRows,
        boolean duplicatesTruncated) {

    public static ImportFileResultDTO failed(String fileName, String message) {
        return new ImportFileResultDTO(fileName, ImportStatus.FAILED, message, null,
                0, 0, 0, 0, 0, null, null,
                List.of(), List.of(), List.of(), false, List.of(), false);
    }
}