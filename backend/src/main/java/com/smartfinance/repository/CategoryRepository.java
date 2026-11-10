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

    // One category, but only if the user is allowed to see it (system default or their own)
    @Query("""
           select c from Category c
           left join c.user u
           where c.id = :id and (u is null or u.id = :userId)
           """)
    Optional<Category> findAccessibleById(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByNameIgnoreCaseAndUserIsNull(String name);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
