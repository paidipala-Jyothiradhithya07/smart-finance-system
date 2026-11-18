package com.smartfinance.analysis;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlyFigures(
        YearMonth month,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal savings,
        long transactionCount,
        BigDecimal openingBalance,
        BalanceSource openingSource,
        BigDecimal closingBalance,
        BalanceSource closingSource,
        List<String> warnings) {

    public boolean balanceAvailable() {
        return openingBalance != null && closingBalance != null;
    }
}