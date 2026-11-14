package com.smartfinance.statement.normalize;

public enum DateOrder {
    DAY_FIRST,      // 25/12/2026 style, certain
    MONTH_FIRST,    // 12/25/2026 style, certain
    AMBIGUOUS,      // like 03/04/2026 with nothing to settle it. Read as day first.
    CONFLICT   
}
