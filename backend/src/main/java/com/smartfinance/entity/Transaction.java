package com.smartfinance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.smartfinance.entity.enums.TransactionSource;
import com.smartfinance.entity.enums.TransactionType;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class Transaction {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "user_id", nullable = false)
	    private User user;

	    // NULL for manually entered transactions
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "bank_statement_id")
	    private BankStatement bankStatement;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "category_id", nullable = false)
	    private Category category;

	    @Column(name = "transaction_date", nullable = false)
	    private LocalDate transactionDate;

	    @Column(nullable = false, length = 500)
	    private String description;

	    // Always positive. Direction is carried by "type".
	    @Column(nullable = false, precision = 15, scale = 2)
	    private BigDecimal amount;

	    @Enumerated(EnumType.STRING)
	    @JdbcTypeCode(SqlTypes.VARCHAR)
	    @Column(nullable = false, length = 10)
	    private TransactionType type;

	    @Column(name = "balance_after", precision = 15, scale = 2)
	    private BigDecimal balanceAfter;

	    @Column(name = "payment_method", length = 30)
	    private String paymentMethod;

	    @Enumerated(EnumType.STRING)
	    @JdbcTypeCode(SqlTypes.VARCHAR)
	    @Column(nullable = false, length = 20)
	    private TransactionSource source;

	    @Column(name = "reference_no", length = 100)
	    private String referenceNo;

	    @Column(name = "row_sequence")
	    private Integer rowSequence;

	    @Column(name = "dedup_hash", length = 64)
	    private String dedupHash;

	    @CreationTimestamp
	    @Column(name = "created_at", nullable = false, updatable = false)
	    private LocalDateTime createdAt;

	    @UpdateTimestamp
	    @Column(name = "updated_at", nullable = false)
	    private LocalDateTime updatedAt;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public BankStatement getBankStatement() {
			return bankStatement;
		}

		public void setBankStatement(BankStatement bankStatement) {
			this.bankStatement = bankStatement;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}

		public LocalDate getTransactionDate() {
			return transactionDate;
		}

		public void setTransactionDate(LocalDate transactionDate) {
			this.transactionDate = transactionDate;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public TransactionType getType() {
			return type;
		}

		public void setType(TransactionType type) {
			this.type = type;
		}

		public BigDecimal getBalanceAfter() {
			return balanceAfter;
		}

		public void setBalanceAfter(BigDecimal balanceAfter) {
			this.balanceAfter = balanceAfter;
		}

		public String getPaymentMethod() {
			return paymentMethod;
		}

		public void setPaymentMethod(String paymentMethod) {
			this.paymentMethod = paymentMethod;
		}

		public TransactionSource getSource() {
			return source;
		}

		public void setSource(TransactionSource source) {
			this.source = source;
		}

		public String getReferenceNo() {
			return referenceNo;
		}

		public void setReferenceNo(String referenceNo) {
			this.referenceNo = referenceNo;
		}

		public Integer getRowSequence() {
			return rowSequence;
		}

		public void setRowSequence(Integer rowSequence) {
			this.rowSequence = rowSequence;
		}

		public String getDedupHash() {
			return dedupHash;
		}

		public void setDedupHash(String dedupHash) {
			this.dedupHash = dedupHash;
		}

		public LocalDateTime getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(LocalDateTime createdAt) {
			this.createdAt = createdAt;
		}

		public LocalDateTime getUpdatedAt() {
			return updatedAt;
		}

		public void setUpdatedAt(LocalDateTime updatedAt) {
			this.updatedAt = updatedAt;
		}

	    
}
