package com.smartfinance.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfinance.dto.request.CategoryRequestDTO;
import com.smartfinance.dto.response.CategoryResponseDTO;
import com.smartfinance.entity.Category;
import com.smartfinance.entity.enums.CategoryType;
import com.smartfinance.exception.ConflictException;
import com.smartfinance.exception.OperationNotAllowedException;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.mapper.CategoryMapper;
import com.smartfinance.repository.BudgetRepository;
import com.smartfinance.repository.CategoryRepository;
import com.smartfinance.repository.TransactionRepository;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

	private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               TransactionRepository transactionRepository,
                               BudgetRepository budgetRepository,
                               UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> list(Long userId, CategoryType type) {
        return categoryRepository.findAvailableForUser(userId).stream()
                .filter(c -> type == null
                        || c.getType() == CategoryType.BOTH
                        || c.getType() == type)
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponseDTO create(Long userId, CategoryRequestDTO request) {
        String name = request.name().trim();
        assertNameAvailable(userId, name, null);

        Category category = new Category();
        category.setUser(userRepository.getReferenceById(userId));
        category.setName(name);
        category.setType(request.type());

        category = saveOrConflict(category);
        log.info("User id={} created category id={}", userId, category.getId());
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponseDTO update(Long userId, Long id, CategoryRequestDTO request) {
        Category category = findOwnCustomCategory(userId, id);
        String name = request.name().trim();
        assertNameAvailable(userId, name, id);

        boolean typeChanged = category.getType() != request.type();
        if (typeChanged && isInUse(id)) {
            throw new ConflictException(
                    "The type cannot change because this category is already used by transactions or budgets");
        }

        category.setName(name);
        category.setType(request.type());
        category = saveOrConflict(category);
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Category category = findOwnCustomCategory(userId, id);
        if (isInUse(id)) {
            throw new ConflictException(
                    "This category is used by transactions or budgets and cannot be deleted");
        }
        categoryRepository.delete(category);
        log.info("User id={} deleted category id={}", userId, id);
    }

    // A category the user cannot see (someone else's) is reported as "not found".
    // A system default is visible but read-only.
    private Category findOwnCustomCategory(Long userId, Long id) {
        Category category = categoryRepository.findAccessibleById(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.getUser() == null) {
            throw new OperationNotAllowedException("System categories cannot be changed or deleted");
        }
        return category;
    }

    private void assertNameAvailable(Long userId, String name, Long excludeId) {
        boolean clashesWithSystem = categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name);
        boolean clashesWithOwn = excludeId == null
                ? categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name)
                : categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, excludeId);
        if (clashesWithSystem || clashesWithOwn) {
            throw new ConflictException("A category with this name already exists");
        }
    }

    private boolean isInUse(Long categoryId) {
        return transactionRepository.existsByCategoryId(categoryId)
                || budgetRepository.existsByCategoryId(categoryId);
    }

    // The database unique key is the final safety net if two requests race
    private Category saveOrConflict(Category category) {
        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A category with this name already exists");
        }
    }
}
