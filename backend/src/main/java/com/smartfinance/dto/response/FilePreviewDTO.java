package com.smartfinance.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record FilePreviewDTO(
        String fileName,
        String status,                 // OK or FAILED
        String fileError,              // reason, only when FAILED
        String fileType,
        Integer headerRow,
        Map<String, String> detectedColumns,
        int totalDataRows,
        int validRows,
        int invalidRows,
        int skippedRows,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<MonthCountDTO> monthsDetected,
        List<String> warnings,
        List<RowErrorDTO> errors,
        boolean errorsTruncated,
        List<PreviewTransactionDTO> sample) {

    public static FilePreviewDTO failed(String fileName, String reason) {
        return new FilePreviewDTO(fileName, "FAILED", reason, null, null, Map.of(),
                0, 0, 0, 0, null, null, List.of(), List.of(), List.of(), false, List.of());
    }
}
