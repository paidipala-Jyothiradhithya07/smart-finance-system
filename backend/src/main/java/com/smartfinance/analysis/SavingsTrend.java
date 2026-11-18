package com.smartfinance.analysis;

public enum SavingsTrend {
    INCREASING,         // savings never fell and rose at least once
    DECREASING,         // savings never rose and fell at least once
    STABLE,             // savings were identical every month
    MIXED,              // went up and down
    INSUFFICIENT_DATA   // fewer than two months
}