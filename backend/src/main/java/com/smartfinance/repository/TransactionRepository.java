package com.smartfinance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartfinance.entity.Transaction;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

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

    long countByBankStatementId(Long bankStatementId);

    // Which of these fingerprints does this user already have? Used to find duplicates.
    @Query("select t.dedupHash from Transaction t where t.user.id = :userId and t.dedupHash in :hashes")
    List<String> findExistingDedupHashes(@Param("userId") Long userId,
                                         @Param("hashes") Collection<String> hashes);

    @Query("select t.transactionDate from Transaction t where t.bankStatement.id = :statementId")
    List<LocalDate> findDatesByBankStatementId(@Param("statementId") Long statementId);

    // ---------- financial analysis ----------

    @Query("select min(t.transactionDate) from Transaction t where t.user.id = :userId")
    Optional<LocalDate> findEarliestTransactionDate(@Param("userId") Long userId);

    @Query("select max(t.transactionDate) from Transaction t where t.user.id = :userId")
    Optional<LocalDate> findLatestTransactionDate(@Param("userId") Long userId);

    // One row per (year, month, type): [year, month, type, sum of amount, number of rows]. The database does the adding.
    @Query("""
           select extract(year from t.transactionDate), extract(month from t.transactionDate),
                  t.type, sum(t.amount), count(t.id)
           from Transaction t
           where t.user.id = :userId and t.transactionDate between :fromDate and :toDate
           group by extract(year from t.transactionDate), extract(month from t.transactionDate), t.type
           """)
    List<Object[]> sumByMonthAndType(@Param("userId") Long userId,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate);

    // Only the rows that carry a bank balance, as [date, type, amount, balance after], in true time order
    @Query("""
           select t.transactionDate, t.type, t.amount, t.balanceAfter
           from Transaction t
           where t.user.id = :userId and t.balanceAfter is not null
             and t.transactionDate between :fromDate and :toDate
           order by t.transactionDate asc, t.rowSequence asc, t.id asc
           """)
    List<Object[]> findBalancePoints(@Param("userId") Long userId,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate);

    // [category id, category name, type, sum of amount, number of rows]
    @Query("""
           select c.id, c.name, t.type, sum(t.amount), count(t.id)
           from Transaction t join t.category c
           where t.user.id = :userId and t.transactionDate between :fromDate and :toDate
           group by c.id, c.name, t.type
           """)
    List<Object[]> sumByCategoryAndType(@Param("userId") Long userId,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate);
}