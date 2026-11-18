package com.smartfinance.service;

import java.time.YearMonth;

import com.smartfinance.dto.response.CategorySummaryResponse;
import com.smartfinance.dto.response.MonthlySummaryDTO;
import com.smartfinance.dto.response.MonthlySummaryResponse;
import com.smartfinance.dto.response.OverviewResponse;

public interface FinanceService {

    MonthlySummaryResponse monthlySummary(Long userId, YearMonth from, YearMonth to);

    MonthlySummaryDTO monthlySummaryForMonth(Long userId, int year, int month);

    OverviewResponse overview(Long userId, YearMonth from, YearMonth to);

    CategorySummaryResponse categorySummary(Long userId, YearMonth from, YearMonth to);
}