package com.smartfinance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfinance.dto.response.CategorySummaryResponse;
import com.smartfinance.dto.response.MonthlySummaryDTO;
import com.smartfinance.dto.response.MonthlySummaryResponse;
import com.smartfinance.entity.User;
import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.repository.TransactionRepository;
import com.smartfinance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
 class FinanceServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FinanceServiceImpl service;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setCurrency("INR");
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    // One month of data: +500 then -200 with bank balances, so opening 1000 and closing 1300
    private void stubJanuaryData() {
        when(transactionRepository.findEarliestTransactionDate(1L)).thenReturn(Optional.of(LocalDate.of(2026, 1, 1)));
        when(transactionRepository.findLatestTransactionDate(1L)).thenReturn(Optional.of(LocalDate.of(2026, 1, 5)));
        when(transactionRepository.sumByMonthAndType(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        new Object[] {2026, 1, TransactionType.CREDIT, new BigDecimal("500.00"), 1L},
                        new Object[] {2026, 1, TransactionType.DEBIT, new BigDecimal("200.00"), 1L}));
        when(transactionRepository.findBalancePoints(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        new Object[] {LocalDate.of(2026, 1, 1), TransactionType.CREDIT,
                                new BigDecimal("500.00"), new BigDecimal("1500.00")},
                        new Object[] {LocalDate.of(2026, 1, 5), TransactionType.DEBIT,
                                new BigDecimal("200.00"), new BigDecimal("1300.00")}));
    }

    @Test
    void monthlySummary_buildsTheFiguresFromTheRepositoryRows() {
        stubJanuaryData();

        MonthlySummaryResponse response = service.monthlySummary(1L, null, null);

        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.from()).isEqualTo("2026-01");
        assertThat(response.to()).isEqualTo("2026-01");
        assertThat(response.months()).hasSize(1);
        MonthlySummaryDTO january = response.months().get(0);
        assertThat(january.label()).isEqualTo("January 2026");
        assertThat(january.totalIncome()).isEqualByComparingTo("500");
        assertThat(january.totalExpense()).isEqualByComparingTo("200");
        assertThat(january.savings()).isEqualByComparingTo("300");
        assertThat(january.openingBalance()).isEqualByComparingTo("1000");
        assertThat(january.closingBalance()).isEqualByComparingTo("1300");
        assertThat(january.balanceAvailable()).isTrue();
        assertThat(january.transactionCount()).isEqualTo(2);
        assertThat(january.warnings()).isEmpty();
    }

    @Test
    void noData_andNoRange_givesAnEmptyList() {
        MonthlySummaryResponse response = service.monthlySummary(1L, null, null);

        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.from()).isNull();
        assertThat(response.months()).isEmpty();
    }

    @Test
    void explicitRangeWithNoData_givesZeroMonthsWithUnavailableBalances() {
        MonthlySummaryResponse response = service.monthlySummary(1L, YearMonth.of(2026, 1), YearMonth.of(2026, 2));

        assertThat(response.months()).hasSize(2);
        assertThat(response.months()).allSatisfy(month -> {
            assertThat(month.totalIncome()).isEqualByComparingTo("0");
            assertThat(month.totalExpense()).isEqualByComparingTo("0");
            assertThat(month.balanceAvailable()).isFalse();
            assertThat(month.openingBalanceSource()).isEqualTo("UNAVAILABLE");
        });
    }

    @Test
    void aMonthAfterTheData_carriesTheLastClosingBalanceForward() {
        stubJanuaryData();

        MonthlySummaryDTO april = service.monthlySummaryForMonth(1L, 2026, 4);

        assertThat(april.transactionCount()).isZero();
        assertThat(april.totalIncome()).isEqualByComparingTo("0");
        assertThat(april.openingBalance()).isEqualByComparingTo("1300");
        assertThat(april.closingBalance()).isEqualByComparingTo("1300");
        assertThat(april.closingBalanceSource()).isEqualTo("CARRIED_FORWARD");
    }

    @Test
    void fromAfterTo_isRejected() {
        assertThatThrownBy(() -> service.monthlySummary(1L, YearMonth.of(2026, 3), YearMonth.of(2026, 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must not be after");
    }

    @Test
    void aRangeOfMoreThan60Months_isRejected() {
        assertThatThrownBy(() -> service.monthlySummary(1L, YearMonth.of(2020, 1), YearMonth.of(2026, 12)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 60 months");
    }

    @Test
    void invalidYearOrMonth_isRejected() {
        assertThatThrownBy(() -> service.monthlySummaryForMonth(1L, 2026, 13))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("month");
        assertThatThrownBy(() -> service.monthlySummaryForMonth(1L, 1800, 5))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("year");
    }

    @Test
    void categorySummary_sortsByAmountAndCalculatesPercentages() {
        when(transactionRepository.findEarliestTransactionDate(1L)).thenReturn(Optional.of(LocalDate.of(2026, 1, 1)));
        when(transactionRepository.findLatestTransactionDate(1L)).thenReturn(Optional.of(LocalDate.of(2026, 1, 31)));
        when(transactionRepository.sumByCategoryAndType(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        new Object[] {9L, "Rent", TransactionType.DEBIT, new BigDecimal("250.00"), 1L},
                        new Object[] {6L, "Food", TransactionType.DEBIT, new BigDecimal("750.00"), 3L},
                        new Object[] {1L, "Salary", TransactionType.CREDIT, new BigDecimal("5000.00"), 1L}));

        CategorySummaryResponse response = service.categorySummary(1L, null, null);

        assertThat(response.totalExpense()).isEqualByComparingTo("1000");
        assertThat(response.totalIncome()).isEqualByComparingTo("5000");
        assertThat(response.expenseCategories()).hasSize(2);
        assertThat(response.expenseCategories().get(0).category()).isEqualTo("Food");
        assertThat(response.expenseCategories().get(0).percentage()).isEqualByComparingTo("75.00");
        assertThat(response.expenseCategories().get(0).transactionCount()).isEqualTo(3);
        assertThat(response.expenseCategories().get(1).category()).isEqualTo("Rent");
        assertThat(response.expenseCategories().get(1).percentage()).isEqualByComparingTo("25.00");
        assertThat(response.incomeCategories()).hasSize(1);
        assertThat(response.incomeCategories().get(0).percentage()).isEqualByComparingTo("100.00");
    }
}