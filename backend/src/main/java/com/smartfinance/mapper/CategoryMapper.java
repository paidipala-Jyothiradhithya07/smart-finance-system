package com.smartfinance.mapper;

import com.smartfinance.dto.response.CategoryResponseDTO;
import com.smartfinance.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponseDTO toResponse(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getUser() == null);
}
}
