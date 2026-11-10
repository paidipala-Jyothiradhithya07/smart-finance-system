package com.smartfinance.dto.request;

import com.smartfinance.entity.enums.CategoryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(

        @NotBlank(message = "Category name is required")
        @Size(max = 60, message = "Category name must be at most 60 characters")
        String name,

        @NotNull(message = "Category type is required (INCOME, EXPENSE or BOTH)")
        CategoryType type) {
}
