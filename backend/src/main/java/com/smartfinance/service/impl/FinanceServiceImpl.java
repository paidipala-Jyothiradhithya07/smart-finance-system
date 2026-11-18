package com.smartfinance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfinance.analysis.BalancePoint;
import com.smartfinance.analysis.MonthTotals;
import com.smartfinance.analysis.MonthlyFigures;
import com.smartfinance.analysis.MonthlySummaryCalculator;
import com.smartfinance.analysis.Overview;
import com.smartfinance.analysis.OverviewCalculator;
import com.smartfinance.dto.response.CategoryAmountDTO;
import com.smartfinance.dto.response.CategorySummaryResponse;
import com.smartfinance.dto.response.MonthlySummaryDTO;
import com.smartfinance.dto.response.MonthlySummaryResponse;
import com.smartfinance.dto.response.OverviewResponse;
import com.smartfinance.entity.User;
import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.mapper.FinanceMapper;
import com.smartfinance.repository.TransactionRepository;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.service.FinanceService;

@Service
public class FinanceServiceImpl implements FinanceService {

    private static final int MAX_RANGE_MONTHS = 60;       // the most a single request may ask for
    private static final int MAX_HISTORY_MONTHS = 240;    // how far back we look to carry balances forward
    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private record Range(YearMonth from, YearMonth to) {
    }

    private record Context(String currency, Range range, YearMonth dataFirst) {
    }

    private record Computation(String currency, Range range, List<MonthlyFigures> figures) {
    }

    private record CategoryAmount(Long id, String name, BigDecimal amount, long count) {
    }

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public FinanceServiceImpl(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryResponse monthlySummary(Long userId, YearMonth from, YearMonth to) {
        Computation computation = compute(userId, from, to);
        Range range = computation.range();
        return FinanceMapper.toMonthlySummaryResponse(computation.currency(),
                range == null ? null : range.from(), range == null ? null : range.to(),
                computation.figures());
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryDTO monthlySummaryForMonth(Long userId, int year, int month) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new InvalidRequestException("year must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (month < 1 || month > 12) {
            throw new InvalidRequestException("month must be between 1 and 12");
        }
        YearMonth target = YearMonth.of(year, month);
        Computation computation = compute(userId, target, target);
        return FinanceMapper.toDto(computation.figures().get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewResponse overview(Long userId, YearMonth from, YearMonth to) {
        Computation computation = compute(userId, from, to);
        Overview overview = OverviewCalculator.calculate(computation.figures());
        Range range = computation.range();
        return FinanceMapper.toOverviewResponse(computation.currency(),
                range == null ? null : range.from(), range == null ? null : range.to(),
                overview, computation.figures());
    }

    @Override
    @Transactional(readOnly = true)
    public CategorySummaryResponse categorySummary(Long userId, YearMonth from, YearMonth to) {
        Context context = resolve(userId, from, to);
        Range range = context.range();
        if (range == null) {
            return new CategorySummaryResponse(context.currency(), null, null, ZERO, ZERO, List.of(), List.of());
        }

        List<Object[]> rows = transactionRepository.sumByCategoryAndType(
                userId, range.from().atDay(1), range.to().atEndOfMonth());

        List<CategoryAmount> expenses = new ArrayList<>();
        List<CategoryAmount> incomes = new ArrayList<>();
        BigDecimal totalExpense = ZERO;
        BigDecimal totalIncome = ZERO;
        for (Object[] row : rows) {
            CategoryAmount item = new CategoryAmount(((Number) row[0]).longValue(), (String) row[1],
                    toBigDecimal(row[3]).setScale(2, RoundingMode.HALF_UP), ((Number) row[4]).longValue());
            if (toType(row[2]) == TransactionType.CREDIT) {
                incomes.add(item);
                totalIncome = totalIncome.add(item.amount());
            } else {
                expenses.add(item);
                totalExpense = totalExpense.add(item.amount());
            }
        }

        return new CategorySummaryResponse(context.currency(), range.from().toString(), range.to().toString(),
                totalIncome, totalExpense, toDtos(expenses, totalExpense), toDtos(incomes, totalIncome));
    }

    // ---------- the shared calculation ----------

    private Computation compute(Long userId, YearMonth requestedFrom, YearMonth requestedTo) {
        Context context = resolve(userId, requestedFrom, requestedTo);
        Range range = context.range();
        if (range == null) {
            return new Computation(context.currency(), null, List.of());
        }

        // Start at the first month with data, even if it is before 'from', so balances can be carried forward
        YearMonth start = context.dataFirst() != null && context.dataFirst().isBefore(range.from())
                ? context.dataFirst() : range.from();
        YearMonth earliestAllowed = range.to().minusMonths(MAX_HISTORY_MONTHS - 1L);
        if (start.isBefore(earliestAllowed)) {
            start = earliestAllowed;
        }

        LocalDate fromDate = start.atDay(1);
        LocalDate toDate = range.to().atEndOfMonth();
        List<MonthTotals> totals = toTotals(transactionRepository.sumByMonthAndType(userId, fromDate, toDate));
        List<BalancePoint> points = toPoints(transactionRepository.findBalancePoints(userId, fromDate, toDate));

        List<MonthlyFigures> figures = MonthlySummaryCalculator.calculate(start, range.to(), totals, points)
                .stream()
                .filter(f -> !f.month().isBefore(range.from()))
                .toList();
        return new Computation(context.currency(), range, figures);
    }

    private Context resolve(Long userId, YearMonth requestedFrom, YearMonth requestedTo) {
        YearMonth dataFirst = transactionRepository.findEarliestTransactionDate(userId)
                .map(YearMonth::from).orElse(null);
        YearMonth dataLast = transactionRepository.findLatestTransactionDate(userId)
                .map(YearMonth::from).orElse(null);
        Range range = resolveRange(requestedFrom, requestedTo, dataFirst, dataLast);
        String currency = userRepository.findById(userId)
                .map(User::getCurrency)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new Context(currency, range, dataFirst);
    }

    /** Fills in missing from/to from the data, and rejects ranges that make no sense. Null means "no data". */
    private static Range resolveRange(YearMonth requestedFrom, YearMonth requestedTo,
                                      YearMonth dataFirst, YearMonth dataLast) {
        YearMonth from = requestedFrom != null ? requestedFrom : dataFirst;
        YearMonth to = requestedTo != null ? requestedTo : dataLast;

        if (from == null && to == null) {
            return null;                      // no data and nothing was requested
        }
        if (to == null) {
            to = from;
        }
        if (from == null) {
            from = to;
        }
        if (requestedTo == null && to.isBefore(from)) {
            to = from;                        // asked from a month after the data ends
        }
        if (requestedFrom == null && from.isAfter(to)) {
            from = to;                        // asked up to a month before the data starts
        }
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        if (ChronoUnit.MONTHS.between(from, to) + 1 > MAX_RANGE_MONTHS) {
            if (requestedFrom == null) {
                from = to.minusMonths(MAX_RANGE_MONTHS - 1L);     // long history: show the latest 5 years
            } else {
                throw new InvalidRequestException("The range can cover at most " + MAX_RANGE_MONTHS + " months");
            }
        }
        return new Range(from, to);
    }

    // ---------- turning database rows into calculator input ----------

    private static List<MonthTotals> toTotals(List<Object[]> rows) {
        Map<YearMonth, MonthTotals> byMonth = new TreeMap<>();
        for (Object[] row : rows) {
            YearMonth month = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            TransactionType type = toType(row[2]);
            BigDecimal sum = toBigDecimal(row[3]);
            long count = ((Number) row[4]).longValue();

            MonthTotals current = byMonth.getOrDefault(month, MonthTotals.empty(month));
            BigDecimal income = type == TransactionType.CREDIT ? current.income().add(sum) : current.income();
            BigDecimal expense = type == TransactionType.DEBIT ? current.expense().add(sum) : current.expense();
            byMonth.put(month, new MonthTotals(month, income, expense, current.transactionCount() + count));
        }
        return new ArrayList<>(byMonth.values());
    }

    private static List<BalancePoint> toPoints(List<Object[]> rows) {
        List<BalancePoint> points = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            points.add(new BalancePoint((LocalDate) row[0], toType(row[1]),
                    toBigDecimal(row[2]), toBigDecimal(row[3])));
        }
        return points;
    }

    private static List<CategoryAmountDTO> toDtos(List<CategoryAmount> items, BigDecimal total) {
        Comparator<CategoryAmount> byAmount = (a, b) -> b.amount().compareTo(a.amount());
        return items.stream()
                .sorted(byAmount.thenComparing(CategoryAmount::name))
                .map(item -> new CategoryAmountDTO(item.id(), item.name(), item.amount(),
                        percentage(item.amount(), total), item.count()))
                .toList();
    }

    private static BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return ZERO;
        }
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal toBigDecimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }

    private static TransactionType toType(Object value) {
        return value instanceof TransactionType type ? type : TransactionType.valueOf(String.valueOf(value));
    }
}