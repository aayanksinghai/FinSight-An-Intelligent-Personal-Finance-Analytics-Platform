package com.finsight.budget.api;

import com.finsight.budget.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("monthYear") String monthYear) {
        
        String ownerEmail = jwt.getSubject();
        List<BudgetResponse> budgets = budgetService.getBudgetsForMonth(ownerEmail, monthYear);
        return ResponseEntity.ok(budgets);
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> upsertBudget(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BudgetRequest request) {
        
        String ownerEmail = jwt.getSubject();
        BudgetResponse response = budgetService.upsertBudget(ownerEmail, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") java.util.UUID id) {
        
        String ownerEmail = jwt.getSubject();
        budgetService.deleteBudget(ownerEmail, id);
        return ResponseEntity.noContent().build();
    }
}
