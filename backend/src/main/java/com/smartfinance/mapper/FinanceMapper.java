package com.smartfinance.mapper;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import com.smartfinance.analysis.MonthAmount;
import com.smartfinance.analysis.MonthlyFigures;
import com.smartfinance.analysis.Overview;
import com.smartfinance.analysis.SavingsChange;
import com.smartfinance.dto.response.MonthAmountDTO;
import com.smartfinance.dto.response.MonthlySummaryDTO;
import com.smartfinance.dto.response.MonthlySummaryResponse;
import com.smartfinance.dto.response.OverviewResponse;
import com.smartfinance.dto.response.SavingsChangeDTO;

public final class FinanceMapper {

    private FinanceMapper() {
    }

    public static String label(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    public static MonthlySummaryDTO toDto(MonthlyFigures f) {
        return new MonthlySummaryDTO(
                f.month().getYear(), f.month().getMonthValue(), label(f.month()),
                f.income(), f.expense(), f.savings(),
                f.openingBalance(), f.closingBalance(), f.balanceAvailable(),
                f.openingSource().name(), f.closingSource().name(),
                f.transactionCount(), f.warnings());
    }

    public static MonthlySummaryResponse toMonthlySummaryResponse(String currency, YearMonth from, YearMonth to,
                                                                  List<MonthlyFigures> figures) {
        return new MonthlySummaryResponse(currency, text(from), text(to),
                figures.stream().map(FinanceMapper::toDto).toList());
    }

    public static OverviewResponse toOverviewResponse(String currency, YearMonth from, YearMonth to,
                                                      Overview o, List<MonthlyFigures> figures) {
        List<String> warnings = figures.stream()
                .flatMap(f -> f.warnings().stream().map(w -> label(f.month()) + ": " + w))
                .toList();
        List<SavingsChangeDTO> changes = o.savingsChanges().stream().map(FinanceMapper::toDto).toList();

        return new OverviewResponse(currency, text(from), text(to),
                o.monthCount(), o.monthsWithTransactions(), o.transactionCount(),
                o.totalIncome(), o.totalExpense(), o.totalSavings(),
                o.averageMonthlyIncome(), o.averageMonthlyExpense(), o.averageMonthlySavings(),
                toDto(o.highestSpendingMonth()), toDto(o.highestIncomeMonth()),
                o.savingsTrend().name(), changes,
                o.openingBalance(), o.closingBalance(), warnings);
    }

    private static MonthAmountDTO toDto(MonthAmount amount) {
        return amount == null ? null
                : new MonthAmountDTO(amount.month().toString(), label(amount.month()), amount.amount());
    }

    private static SavingsChangeDTO toDto(SavingsChange change) {
        return new SavingsChangeDTO(change.month().toString(), label(change.month()), change.savings(),
                change.changeFromPrevious(), change.changePercent());
    }

    private static String text(YearMonth month) {
        return month == null ? null : month.toString();
    }
}