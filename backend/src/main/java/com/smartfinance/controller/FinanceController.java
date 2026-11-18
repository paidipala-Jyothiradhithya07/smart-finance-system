package com.smartfinance.controller;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfinance.dto.response.CategorySummaryResponse;
import com.smartfinance.dto.response.MonthlySummaryDTO;
import com.smartfinance.dto.response.MonthlySummaryResponse;
import com.smartfinance.dto.response.OverviewResponse;
import com.smartfinance.security.AuthenticatedUser;
import com.smartfinance.service.FinanceService;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/monthly-summary")
    public MonthlySummaryResponse monthlySummary(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return financeService.monthlySummary(principal.getId(), from, to);
    }

    @GetMapping("/monthly-summary/{year}/{month}")
    public MonthlySummaryDTO monthlySummaryForMonth(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @PathVariable int year, @PathVariable int month) {
        return financeService.monthlySummaryForMonth(principal.getId(), year, month);
    }

    @GetMapping("/overview")
    public OverviewResponse overview(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return financeService.overview(principal.getId(), from, to);
    }

    @GetMapping("/category-summary")
    public CategorySummaryResponse categorySummary(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return financeService.categorySummary(principal.getId(), from, to);
    }
}