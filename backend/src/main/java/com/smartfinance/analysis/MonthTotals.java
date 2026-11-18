package com.smartfinance.analysis;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthTotals(YearMonth month, BigDecimal income, BigDecimal expense, long transactionCount) {

    public static MonthTotals empty(YearMonth month) {
        return new MonthTotals(month, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
}