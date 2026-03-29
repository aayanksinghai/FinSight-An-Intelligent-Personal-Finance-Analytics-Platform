package com.finsight.budget.persistence;

import com.finsight.budget.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    
    List<Budget> findByOwnerEmailAndMonthYear(String ownerEmail, String monthYear);
    
    Optional<Budget> findByOwnerEmailAndCategoryIdAndMonthYear(String ownerEmail, String categoryId, String monthYear);
}
