package com.smartfinance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.smartfinance.dto.request.TransactionFilter;
import com.smartfinance.dto.request.TransactionRequestDTO;
import com.smartfinance.dto.response.PageResponseDTO;
import com.smartfinance.dto.response.TransactionResponseDTO;
import com.smartfinance.entity.Category;
import com.smartfinance.entity.Transaction;
import com.smartfinance.entity.User;
import com.smartfinance.entity.enums.CategoryType;
import com.smartfinance.entity.enums.TransactionSource;
import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.repository.CategoryRepository;
import com.smartfinance.repository.TransactionRepository;
import com.smartfinance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {
	 @Mock
	    private TransactionRepository transactionRepository;
	    @Mock
	    private CategoryRepository categoryRepository;
	    @Mock
	    private UserRepository userRepository;

	    @InjectMocks
	    private TransactionServiceImpl service;

	    // ---------- create ----------

	    @Test
	    void create_savesManualTransactionForTheLoggedInUser() {
	        Category food = category(6L, "Food", CategoryType.EXPENSE);
	        when(categoryRepository.findAccessibleById(6L, 1L)).thenReturn(Optional.of(food));
	        when(userRepository.getReferenceById(1L)).thenReturn(new User());
	        when(transactionRepository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
	            Transaction t = invocation.getArgument(0);
	            t.setId(10L);
	            return t;
	        });

	        TransactionRequestDTO request = new TransactionRequestDTO(
	                LocalDate.of(2026, 10, 5), "  SWIGGY  ", new BigDecimal("850"),
	                TransactionType.DEBIT, 6L, null, null);

	        TransactionResponseDTO response = service.create(1L, request);

	        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
	        verify(transactionRepository).saveAndFlush(captor.capture());
	        Transaction saved = captor.getValue();

	        assertThat(saved.getSource()).isEqualTo(TransactionSource.MANUAL);
	        assertThat(saved.getDescription()).isEqualTo("SWIGGY");
	        assertThat(saved.getAmount()).isEqualByComparingTo("850.00");
	        assertThat(saved.getAmount().scale()).isEqualTo(2);
	        assertThat(response.id()).isEqualTo(10L);
	        assertThat(response.categoryName()).isEqualTo("Food");
	    }

	    @Test
	    void create_rejectsCategoryThatDoesNotMatchTheType() {
	        Category food = category(6L, "Food", CategoryType.EXPENSE);
	        when(categoryRepository.findAccessibleById(6L, 1L)).thenReturn(Optional.of(food));

	        TransactionRequestDTO request = new TransactionRequestDTO(
	                LocalDate.of(2026, 10, 5), "Bad", new BigDecimal("100"),
	                TransactionType.CREDIT, 6L, null, null);

	        assertThatThrownBy(() -> service.create(1L, request))
	                .isInstanceOf(InvalidRequestException.class)
	                .hasMessageContaining("cannot be used for a CREDIT");
	        verify(transactionRepository, never()).saveAndFlush(any());
	    }

	    @Test
	    void create_unknownCategory_throwsNotFound() {
	        when(categoryRepository.findAccessibleById(9999L, 1L)).thenReturn(Optional.empty());

	        TransactionRequestDTO request = new TransactionRequestDTO(
	                LocalDate.of(2026, 10, 5), "X", new BigDecimal("100"),
	                TransactionType.DEBIT, 9999L, null, null);

	        assertThatThrownBy(() -> service.create(1L, request))
	                .isInstanceOf(ResourceNotFoundException.class)
	                .hasMessage("Category not found");
	    }

	    // ---------- read / delete (user isolation) ----------

	    @Test
	    void getById_otherUsersTransaction_isReportedAsNotFound() {
	        when(transactionRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

	        assertThatThrownBy(() -> service.getById(2L, 5L))
	                .isInstanceOf(ResourceNotFoundException.class)
	                .hasMessage("Transaction not found");
	        verify(transactionRepository, never()).findById(any());
	    }

	    @Test
	    void delete_removesOwnTransaction() {
	        Transaction existing = transaction(3L, category(6L, "Food", CategoryType.EXPENSE));
	        when(transactionRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(existing));

	        service.delete(1L, 3L);

	        verify(transactionRepository).delete(existing);
	    }

	    // ---------- update (imported rows are protected) ----------

	    @Test
	    void update_importedTransaction_cannotChangeTheAmount() {
	        Category food = category(6L, "Food", CategoryType.EXPENSE);
	        Transaction imported = transaction(7L, food);
	        imported.setSource(TransactionSource.BANK_STATEMENT);
	        when(transactionRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(imported));
	        when(categoryRepository.findAccessibleById(6L, 1L)).thenReturn(Optional.of(food));

	        TransactionRequestDTO request = new TransactionRequestDTO(
	                LocalDate.of(2026, 10, 5), "UBER", new BigDecimal("600"),
	                TransactionType.DEBIT, 6L, null, null);

	        assertThatThrownBy(() -> service.update(1L, 7L, request))
	                .isInstanceOf(InvalidRequestException.class)
	                .hasMessageContaining("Imported transactions");
	        verify(transactionRepository, never()).saveAndFlush(any());
	    }

	    @Test
	    void update_importedTransaction_canChangeTheDescription() {
	        Category food = category(6L, "Food", CategoryType.EXPENSE);
	        Transaction imported = transaction(7L, food);
	        imported.setSource(TransactionSource.BANK_STATEMENT);
	        when(transactionRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(imported));
	        when(categoryRepository.findAccessibleById(6L, 1L)).thenReturn(Optional.of(food));
	        when(transactionRepository.saveAndFlush(any(Transaction.class)))
	                .thenAnswer(invocation -> invocation.getArgument(0));

	        // same date, amount (500 equals 500.00) and type: only the description changes
	        TransactionRequestDTO request = new TransactionRequestDTO(
	                LocalDate.of(2026, 10, 5), "UBER RIDE", new BigDecimal("500"),
	                TransactionType.DEBIT, 6L, null, null);

	        TransactionResponseDTO response = service.update(1L, 7L, request);

	        assertThat(response.description()).isEqualTo("UBER RIDE");
	    }

	    // ---------- search ----------

	    @Test
	    void search_rejectsPageSizeAboveTheLimit() {
	        assertThatThrownBy(() -> service.search(1L, emptyFilter(), 0, 500, "transactionDate", "desc"))
	                .isInstanceOf(InvalidRequestException.class)
	                .hasMessageContaining("size");
	    }

	    @Test
	    void search_rejectsUnknownSortField() {
	        assertThatThrownBy(() -> service.search(1L, emptyFilter(), 0, 10, "password", "desc"))
	                .isInstanceOf(InvalidRequestException.class)
	                .hasMessageContaining("sortBy");
	    }

	    @Test
	    void search_rejectsFromAfterTo() {
	        TransactionFilter filter = new TransactionFilter(
	                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1), null, null, null, null, null, null);

	        assertThatThrownBy(() -> service.search(1L, filter, 0, 10, "transactionDate", "desc"))
	                .isInstanceOf(InvalidRequestException.class)
	                .hasMessageContaining("'from'");
	    }

	    @Test
	    void search_rejectsMinAmountGreaterThanMaxAmount() {
	        TransactionFilter filter = new TransactionFilter(
	                null, null, null, null, new BigDecimal("500"), new BigDecimal("100"), null, null);

	        assertThatThrownBy(() -> service.search(1L, filter, 0, 10, "transactionDate", "desc"))
	                .isInstanceOf(InvalidRequestException.class)
	                .hasMessageContaining("minAmount");
	    }

	    @Test
	    void search_appliesRequestedPagingAndSorting() {
	        Transaction tx = transaction(1L, category(6L, "Food", CategoryType.EXPENSE));
	        Pageable requested = PageRequest.of(0, 10);
	        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
	                .thenReturn(new PageImpl<>(List.of(tx), requested, 1));

	        PageResponseDTO<TransactionResponseDTO> result =
	                service.search(1L, emptyFilter(), 0, 10, "amount", "asc");

	        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
	        verify(transactionRepository).findAll(ArgumentMatchers.<Specification<Transaction>>any(), captor.capture());
	        Pageable used = captor.getValue();

	        assertThat(used.getPageSize()).isEqualTo(10);
	        assertThat(used.getSort().getOrderFor("amount").getDirection()).isEqualTo(Sort.Direction.ASC);
	        assertThat(used.getSort().getOrderFor("id")).isNotNull();   // stable tiebreaker
	        assertThat(result.totalElements()).isEqualTo(1);
	        assertThat(result.content()).hasSize(1);
	    }

	    // ---------- helpers ----------

	    private static TransactionFilter emptyFilter() {
	        return new TransactionFilter(null, null, null, null, null, null, null, null);
	    }

	    private static Category category(Long id, String name, CategoryType type) {
	        Category category = new Category();
	        category.setId(id);
	        category.setName(name);
	        category.setType(type);
	        return category;
	    }

	    private static Transaction transaction(Long id, Category category) {
	        Transaction t = new Transaction();
	        t.setId(id);
	        t.setCategory(category);
	        t.setTransactionDate(LocalDate.of(2026, 10, 5));
	        t.setDescription("UBER");
	        t.setAmount(new BigDecimal("500.00"));
	        t.setType(TransactionType.DEBIT);
	        t.setSource(TransactionSource.MANUAL);
	        return t;
	    }
}
