package com.smartfinance.mapper;


import com.smartfinance.dto.response.TransactionResponseDTO;
import com.smartfinance.entity.Category;
import com.smartfinance.entity.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponseDTO toResponse(Transaction t) {
        Category category = t.getCategory();
        return new TransactionResponseDTO(
                t.getId(),
                t.getTransactionDate(),
                t.getDescription(),
                t.getAmount(),
                t.getType(),
                category.getId(),
                category.getName(),
                t.getPaymentMethod(),
                t.getSource(),
                t.getReferenceNo(),
                t.getBalanceAfter(),
                t.getBankStatement() != null ? t.getBankStatement().getId() : null,
                t.getCreatedAt());
    }
}
