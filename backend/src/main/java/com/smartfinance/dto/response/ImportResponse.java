package com.smartfinance.dto.response;

import java.util.List;

public record ImportResponse(
        int filesReceived,
        int filesImported,
        int transactionsImported,
        int duplicatesSkipped,
        int invalidRows,
        int monthsImported,
        String message,
        List<ImportFileResultDTO> files) {
}