package com.smartfinance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartfinance.dto.request.CategoryRequestDTO;
import com.smartfinance.dto.response.CategoryResponseDTO;
import com.smartfinance.entity.enums.CategoryType;
import com.smartfinance.security.AuthenticatedUser;
import com.smartfinance.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
	private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponseDTO> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                          @RequestParam(required = false) CategoryType type) {
        return categoryService.list(principal.getId(), type);
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @Valid @RequestBody CategoryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public CategoryResponseDTO update(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable Long id,
                                      @Valid @RequestBody CategoryRequestDTO request) {
        return categoryService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        categoryService.delete(principal.getId(), id);
    }
}
