package com.smartfinance.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smartfinance.entity.enums.TransactionType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransactionRequestDTO(

        @NotNull(message = "Transaction date is required")
        @PastOrPresent(message = "Transaction date cannot be in the future")
        LocalDate transactionDate,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        @Digits(integer = 13, fraction = 2, message = "Amount can have at most 13 digits and 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Type is required (CREDIT or DEBIT)")
        TransactionType type,

        @NotNull(message = "Category is required")
        Long categoryId,

        @Size(max = 30, message = "Payment method must be at most 30 characters")
        String paymentMethod,

        @Size(max = 100, message = "Reference number must be at most 100 characters")
        String referenceNo) {
}
