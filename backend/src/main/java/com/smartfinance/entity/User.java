package com.smartfinance.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.smartfinance.entity.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {


	    @Id
	    @GeneratedValue(strategy = GenerationType .IDENTITY)
	    private Long id;

	    @Column(nullable = false, length = 100)
	    private String name;

	    @Column(nullable = false, length = 150)
	    private String email;

	    @Column(name = "password_hash", nullable = false)
	    private String passwordHash;

	    @Enumerated(EnumType.STRING)
	    @JdbcTypeCode(SqlTypes.VARCHAR)
	    @Column(nullable = false, length = 20)
	    private Role role = Role.USER;

	    // Set by the service from app.currency.default (never hardcoded here)
	    @Column(nullable = false, length = 3)
	    private String currency;

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

	    public String getName() {
	    	return name;
	    	}
	    
	    public void setName(String name) { 
	    	this.name = name;
	    	}

	    public String getEmail() { 
	    	return email; 
	    	}
	    public void setEmail(String email) { 
	    	this.email = email;
	    	}

	    public String getPasswordHash() { 
	    	return passwordHash; 
	    	}
	    public void setPasswordHash(String passwordHash) { 
	    	this.passwordHash = passwordHash; 
	    	}

	    public Role getRole() { 
	    	return role; 
	    	}
	    public void setRole(Role role) {
	    	this.role = role; 
	    	}

	    public String getCurrency() { 
	    	return currency; 
	    	}
	    public void setCurrency(String currency) {
	    	this.currency = currency; 
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
