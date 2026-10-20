package com.smartfinance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartfinance.entity.BankStatement;

public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {

    List<BankStatement> findByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<BankStatement> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndFileHash(Long userId, String fileHash);
}
