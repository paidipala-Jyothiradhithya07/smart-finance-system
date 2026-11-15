package com.smartfinance.mapper;

import java.util.List;

import com.smartfinance.dto.response.BankStatementDetailDTO;
import com.smartfinance.dto.response.BankStatementResponseDTO;
import com.smartfinance.dto.response.MonthCountDTO;
import com.smartfinance.entity.BankStatement;

public final class BankStatementMapper {

    private BankStatementMapper() {
    }

    public static BankStatementResponseDTO toResponse(BankStatement s) {
        return new BankStatementResponseDTO(s.getId(), s.getFileName(), s.getStatus(), s.getTotalRows(),
                s.getImportedCount(), s.getDuplicateCount(), s.getInvalidCount(),
                s.getPeriodStart(), s.getPeriodEnd(), s.getUploadedAt());
    }

    public static BankStatementDetailDTO toDetail(BankStatement s, List<MonthCountDTO> months) {
        return new BankStatementDetailDTO(s.getId(), s.getFileName(), s.getStatus(), s.getTotalRows(),
                s.getImportedCount(), s.getDuplicateCount(), s.getInvalidCount(),
                s.getPeriodStart(), s.getPeriodEnd(), s.getUploadedAt(), months);
    }
}