package com.smartfinance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;

@Entity
@Table(name="budgets")
public class Budget {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "user_id", nullable = false)
	    private User user;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "category_id", nullable = false)
	    private Category category;

	    @Column(name = "budget_year", nullable = false)
	    private Integer budgetYear;

	    @Column(name = "budget_month", nullable = false)
	    private Integer budgetMonth;

	    @Column(nullable = false, precision = 15, scale = 2)
	    private BigDecimal amount;

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

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}

		public Integer getBudgetYear() {
			return budgetYear;
		}

		public void setBudgetYear(Integer budgetYear) {
			this.budgetYear = budgetYear;
		}

		public Integer getBudgetMonth() {
			return budgetMonth;
		}

		public void setBudgetMonth(Integer budgetMonth) {
			this.budgetMonth = budgetMonth;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
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
