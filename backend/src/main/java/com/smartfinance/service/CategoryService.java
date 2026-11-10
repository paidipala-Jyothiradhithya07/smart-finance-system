package com.smartfinance.service;

import java.util.List;

import com.smartfinance.dto.request.CategoryRequestDTO;
import com.smartfinance.dto.response.CategoryResponseDTO;
import com.smartfinance.entity.enums.CategoryType;


public interface CategoryService {
	List<CategoryResponseDTO> list(Long userId, CategoryType type);

    CategoryResponseDTO create(Long userId, CategoryRequestDTO request);

    CategoryResponseDTO update(Long userId, Long id, CategoryRequestDTO request);

    void delete(Long userId, Long id);
}
