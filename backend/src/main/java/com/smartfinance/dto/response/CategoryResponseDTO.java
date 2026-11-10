package com.smartfinance.dto.response;

import com.smartfinance.entity.enums.CategoryType;

public record CategoryResponseDTO(
        Long id,
        String name,
        CategoryType type,
        boolean systemDefault) {
}
