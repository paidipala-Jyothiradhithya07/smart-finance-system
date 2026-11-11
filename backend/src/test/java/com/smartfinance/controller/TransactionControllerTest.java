package com.smartfinance.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartfinance.dto.request.TransactionFilter;
import com.smartfinance.dto.request.TransactionRequestDTO;
import com.smartfinance.dto.response.PageResponseDTO;
import com.smartfinance.dto.response.TransactionResponseDTO;
import com.smartfinance.entity.enums.Role;
import com.smartfinance.entity.enums.TransactionSource;
import com.smartfinance.entity.enums.TransactionType;
import com.smartfinance.exception.GlobalExceptionHandler;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.security.AuthenticatedUser;
import com.smartfinance.service.TransactionService;

@ExtendWith(MockitoExtension.class)
public class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        AuthenticatedUser user = new AuthenticatedUser(1L, "asha@example.com", "hash", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_returnsAPage() throws Exception {
        when(transactionService.search(eq(1L), any(TransactionFilter.class),
                eq(0), eq(20), eq("transactionDate"), eq("desc")))
                .thenReturn(new PageResponseDTO<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void list_passesTheFiltersToTheService() throws Exception {
        when(transactionService.search(eq(1L), any(TransactionFilter.class),
                eq(0), eq(20), eq("transactionDate"), eq("desc")))
                .thenReturn(new PageResponseDTO<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/transactions")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("type", "DEBIT")
                        .param("q", "swiggy")
                        .param("minAmount", "100"))
                .andExpect(status().isOk());

        ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(transactionService).search(eq(1L), captor.capture(),
                eq(0), eq(20), eq("transactionDate"), eq("desc"));

        TransactionFilter filter = captor.getValue();
        assertThat(filter.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(filter.to()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(filter.type()).isEqualTo(TransactionType.DEBIT);
        assertThat(filter.keyword()).isEqualTo("swiggy");
        assertThat(filter.minAmount()).isEqualByComparingTo("100");
        assertThat(filter.categoryId()).isNull();
    }

    @Test
    void list_invalidDate_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions").param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'from'"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        TransactionResponseDTO created = new TransactionResponseDTO(
                10L, LocalDate.of(2026, 10, 5), "SWIGGY", new BigDecimal("850.00"),
                TransactionType.DEBIT, 6L, "Food", null, TransactionSource.MANUAL,
                null, null, null, null);
        when(transactionService.create(eq(1L), any(TransactionRequestDTO.class))).thenReturn(created);

        String body = """
                {
                  "transactionDate": "2026-10-05",
                  "description": "SWIGGY",
                  "amount": 850.00,
                  "type": "DEBIT",
                  "categoryId": 6
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void create_invalidAmountAndDescription_returns400WithFieldErrors() throws Exception {
        String body = """
                {
                  "transactionDate": "2026-10-05",
                  "description": "",
                  "amount": -5,
                  "type": "DEBIT",
                  "categoryId": 6
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.amount").value("Amount must be greater than zero"))
                .andExpect(jsonPath("$.fieldErrors.description").value("Description is required"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(transactionService.getById(1L, 99L))
                .thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(get("/api/transactions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/transactions/5"))
                .andExpect(status().isNoContent());

        verify(transactionService).delete(1L, 5L);
    }
}
