package com.smartfinance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartfinance.entity.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndBudgetYearAndBudgetMonth(Long userId, Integer year, Integer month);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(
            Long userId, Long categoryId, Integer year, Integer month);
}
