package com.smartfinance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfinance.dto.request.TransactionRequestDTO;
import com.smartfinance.dto.response.PageResponseDTO;
import com.smartfinance.dto.response.TransactionResponseDTO;
import com.smartfinance.entity.Category;
import com.smartfinance.entity.Transaction;
import com.smartfinance.entity.enums.CategoryType;
import com.smartfinance.entity.enums.TransactionSource;
import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.mapper.TransactionMapper;
import com.smartfinance.repository.CategoryRepository;
import com.smartfinance.repository.TransactionRepository;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.service.TransactionService;

@Service
public class TransactionServiceImpl implements TransactionService {

	 private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

	    private static final int MAX_PAGE_SIZE = 100;
	    // Only these fields may be used for sorting. A free-text sort field could crash or probe the schema.
	    private static final Set<String> SORTABLE_FIELDS = Set.of("transactionDate", "amount", "createdAt");

	    private final TransactionRepository transactionRepository;
	    private final CategoryRepository categoryRepository;
	    private final UserRepository userRepository;

	    public TransactionServiceImpl(TransactionRepository transactionRepository,
	                                  CategoryRepository categoryRepository,
	                                  UserRepository userRepository) {
	        this.transactionRepository = transactionRepository;
	        this.categoryRepository = categoryRepository;
	        this.userRepository = userRepository;
	    }
	    @Override
	    @Transactional
	    public TransactionResponseDTO create(Long userId, TransactionRequestDTO request) {
	        Category category = resolveCategory(userId, request.categoryId(), request.type());

	        Transaction transaction = new Transaction();
	        transaction.setUser(userRepository.getReferenceById(userId));
	        transaction.setCategory(category);
	        transaction.setSource(TransactionSource.MANUAL);
	        applyFields(transaction, request);

	        transaction = transactionRepository.saveAndFlush(transaction);
	        log.info("User id={} created transaction id={}", userId, transaction.getId());
	        return TransactionMapper.toResponse(transaction);
	    }

	    @Override
	    @Transactional(readOnly = true)
	    public TransactionResponseDTO getById(Long userId, Long id) {
	        return TransactionMapper.toResponse(findOwned(userId, id));
	    }

	    @Override
	    @Transactional(readOnly = true)
	    public PageResponseDTO<TransactionResponseDTO> list(Long userId, int page, int size,
	                                                        String sortBy, String direction) {
	        if (page < 0) {
	            throw new InvalidRequestException("page must be 0 or greater");
	        }
	        if (size < 1 || size > MAX_PAGE_SIZE) {
	            throw new InvalidRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
	        }
	        if (!SORTABLE_FIELDS.contains(sortBy)) {
	            throw new InvalidRequestException("sortBy must be one of: transactionDate, amount, createdAt");
	        }

	        Sort.Direction sortDirection;
	        if ("asc".equalsIgnoreCase(direction)) {
	            sortDirection = Sort.Direction.ASC;
	        } else if ("desc".equalsIgnoreCase(direction)) {
	            sortDirection = Sort.Direction.DESC;
	        } else {
	            throw new InvalidRequestException("direction must be asc or desc");
	        }

	        // The id tiebreaker keeps page boundaries stable when many rows share the same date
	        Sort sort = Sort.by(sortDirection, sortBy).and(Sort.by(Sort.Direction.DESC, "id"));

	        return PageResponseDTO.from(
	                transactionRepository.findByUserId(userId, PageRequest.of(page, size, sort))
	                        .map(TransactionMapper::toResponse));
	    }

	    @Override
	    @Transactional
	    public TransactionResponseDTO update(Long userId, Long id, TransactionRequestDTO request) {
	        Transaction transaction = findOwned(userId, id);
	        Category category = resolveCategory(userId, request.categoryId(), request.type());

	        // Imported rows mirror the bank's record. Changing date, amount or type would break the balances.
	        if (transaction.getSource() == TransactionSource.BANK_STATEMENT) {
	            boolean coreChanged =
	                    !transaction.getTransactionDate().equals(request.transactionDate())
	                    || transaction.getAmount().compareTo(request.amount()) != 0
	                    || transaction.getType() != request.type();
	            if (coreChanged) {
	                throw new InvalidRequestException(
	                        "Imported transactions cannot change date, amount or type. "
	                        + "You can change the category, description and payment method.");
	            }
	        }

	        transaction.setCategory(category);
	        applyFields(transaction, request);

	        transaction = transactionRepository.saveAndFlush(transaction);
	        log.info("User id={} updated transaction id={}", userId, id);
	        return TransactionMapper.toResponse(transaction);
	    }

	    @Override
	    @Transactional
	    public void delete(Long userId, Long id) {
	        Transaction transaction = findOwned(userId, id);
	        transactionRepository.delete(transaction);
	        log.info("User id={} deleted transaction id={}", userId, id);
	    }

	    // The user id is part of the query, so someone else's transaction simply "does not exist" for you
	    private Transaction findOwned(Long userId, Long id) {
	        return transactionRepository.findByIdAndUserId(id, userId)
	                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
	    }

	    private Category resolveCategory(Long userId, Long categoryId, TransactionType type) {
	        Category category = categoryRepository.findAccessibleById(categoryId, userId)
	                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

	        CategoryType categoryType = category.getType();
	        boolean compatible = categoryType == CategoryType.BOTH
	                || (type == TransactionType.CREDIT && categoryType == CategoryType.INCOME)
	                || (type == TransactionType.DEBIT && categoryType == CategoryType.EXPENSE);
	        if (!compatible) {
	            throw new InvalidRequestException("Category '" + category.getName() + "' is for "
	                    + categoryType + " and cannot be used for a " + type + " transaction");
	        }
	        return category;
	    }

	    private void applyFields(Transaction transaction, TransactionRequestDTO request) {
	        transaction.setTransactionDate(request.transactionDate());
	        transaction.setDescription(request.description().trim());
	        transaction.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
	        transaction.setType(request.type());
	        transaction.setPaymentMethod(blankToNull(request.paymentMethod()));
	        transaction.setReferenceNo(blankToNull(request.referenceNo()));
	    }

	    private String blankToNull(String value) {
	        return (value == null || value.isBlank()) ? null : value.trim();
	    }
   
}

