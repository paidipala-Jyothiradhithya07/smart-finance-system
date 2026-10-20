package com.smartfinance.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartfinance.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // System default categories (user is NULL)
    List<Category> findByUserIsNullOrderByNameAsc();

    // Used later by the auto-categorizer to find e.g. "Food" or "Other"
    Optional<Category> findByNameAndUserIsNull(String name);

    // System defaults plus this user's own categories
    @Query("""
           select c from Category c
           left join c.user u
           where u is null or u.id = :userId
           order by c.name
           """)
    List<Category> findAvailableForUser(@Param("userId") Long userId);
}
