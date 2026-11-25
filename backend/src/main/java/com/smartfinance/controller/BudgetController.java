package com.smartfinance.controller;

import com.smartfinance.dto.response.BudgetDTO;
import com.smartfinance.dto.response.CategorySummaryResponse;
import com.smartfinance.dto.response.MonthlySummaryDTO;
import com.smartfinance.dto.response.OverviewResponse;
import com.smartfinance.dto.response.InsightsResponse;
import com.smartfinance.entity.Budget;
import com.smartfinance.entity.Category;
import com.smartfinance.entity.User;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.service.BudgetService;
import com.smartfinance.service.FinanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final FinanceService financeService;

    public BudgetController(BudgetService budgetService, FinanceService financeService) {
        this.budgetService = budgetService;
        this.financeService = financeService;
    }

    @PostMapping
    public ResponseEntity<BudgetDTO> createBudget(@Valid @RequestBody BudgetDTO budgetDTO) {
        Budget budget = budgetService.createBudget(budgetDTO, getCurrentUserId());
        return ResponseEntity.ok(BudgetMapper.toDTO(budget));
    }

    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getBudgets(@RequestParam(required = false) Integer year,
                                                    @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(budgetService.getBudgetsForUser(getCurrentUserId(), year, month));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetDTO> getBudget(@PathVariable Long id) {
        return ResponseEntity.ok(BudgetMapper.toDTO(budgetService.getBudgetById(getCurrentUserId(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDTO> updateBudget(@PathVariable Long id,
                                                @Valid @RequestBody BudgetDTO budgetDTO) {
        Budget budget = budgetService.updateBudget(getCurrentUserId(), id, budgetDTO);
        return ResponseEntity.ok(BudgetMapper.toDTO(budget));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category-summary")
    public ResponseEntity<CategorySummaryResponse> getCategorySummary(@RequestParam(required = false) YearMonth from,
                                                                  @RequestParam(required = false) YearMonth to) {
        return ResponseEntity.ok(financeService.categorySummary(getCurrentUserId(), from, to));
    }

    @GetMapping("/insights")
    public ResponseEntity<InsightsResponse> getInsights(@RequestParam(required = false) YearMonth from,
                                                      @RequestParam(required = false) YearMonth to) {
        return ResponseEntity.ok(financeService.generateInsights(getCurrentUserId(), from, to));
    }

    // Helper method to get current user ID from JWT
    private Long getCurrentUserId() {
        // Implementation would extract user ID from JWT token
        // For simplicity in this example, we'll assume it's available
        return 1L; // TODO: Replace with actual JWT parsing
    }
}