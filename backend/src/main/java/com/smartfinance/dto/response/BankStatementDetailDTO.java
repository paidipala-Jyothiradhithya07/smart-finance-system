package com.smartfinance.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.smartfinance.entity.enums.StatementStatus;

public record BankStatementDetailDTO(
        Long id,
        String fileName,
        StatementStatus status,
        int totalRows,
        int importedCount,
        int duplicateCount,
        int invalidCount,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDateTime uploadedAt,
        List<MonthCountDTO> monthsDetected) {
}