package com.smartfinance.service;

import java.math.BigDecimal;
import java.time.YearMonth;

import com.smartfinance.dto.response.BudgetDTO;
import com.smartfinance.dto.response.BudgetSummaryResponse;

import java.util.List;

public interface BudgetService {

    BudgetDTO createBudget(Long userId, Long categoryId, int year, int month, BigDecimal amount);

    List<BudgetDTO> getBudgetsByUser(Long userId, int year, int month);

    List<BudgetDTO> getBudgetsByCategory(Long categoryId, int year, int month);

    List<BudgetSummaryResponse> getBudgetSummaryByUser(Long userId, int year, int month);

    List<BudgetSummaryResponse> getBudgetSummaryByCategory(Long categoryId, int year, int month);

    boolean updateBudget(Long budgetId, BigDecimal amount);

    boolean deleteBudget(Long budgetId);
}