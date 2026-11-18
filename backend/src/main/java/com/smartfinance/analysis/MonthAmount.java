package com.smartfinance.analysis;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthAmount(YearMonth month, BigDecimal amount) {
}