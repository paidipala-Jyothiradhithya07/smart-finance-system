package com.smartfinance.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.smartfinance.entity.Transaction;

public interface TransactionRepository extends JpaRepository <Transaction, Long>,JpaSpecificationExecutor<Transaction>{

	 // Always filter by user, so one user can never read another user's data.
    // The entity graph loads each category in the same query (avoids one extra query per row).
	  @EntityGraph(attributePaths = "category")
	    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

	    // Search with optional filters. Same entity graph, so the list needs no extra queries.
	    @Override
	    @EntityGraph(attributePaths = "category")
	    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

	    boolean existsByUserIdAndDedupHash(Long userId, String dedupHash);

	    boolean existsByCategoryId(Long categoryId);

	    long countByUserId(Long userId);
}
