package com.smartfinance.analysis;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Savings of one month and how they changed from the month before. The change is null for the first month. */
public record SavingsChange(YearMonth month, BigDecimal savings,
                            BigDecimal changeFromPrevious, BigDecimal changePercent) {
}