package com.smartfinance.repository;

import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;

import com.smartfinance.entity.Transaction;

public interface TransactionRepository extends JpaRepository <Transaction, Long>{

    // Always filter by user, so one user can never read another user's data
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDedupHash(Long userId, String dedupHash);

    long countByUserId(Long userId);
}
