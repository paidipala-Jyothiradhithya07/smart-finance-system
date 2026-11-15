package com.smartfinance.service;

import java.time.YearMonth;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfinance.dto.response.BankStatementDetailDTO;
import com.smartfinance.dto.response.BankStatementResponseDTO;
import com.smartfinance.dto.response.MonthCountDTO;
import com.smartfinance.entity.BankStatement;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.mapper.BankStatementMapper;
import com.smartfinance.repository.BankStatementRepository;
import com.smartfinance.repository.TransactionRepository;

@Service
public class BankStatementService {

    private static final Logger log = LoggerFactory.getLogger(BankStatementService.class);

    private final BankStatementRepository statementRepository;
    private final TransactionRepository transactionRepository;

    public BankStatementService(BankStatementRepository statementRepository,
                                TransactionRepository transactionRepository) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<BankStatementResponseDTO> list(Long userId) {
        return statementRepository.findByUserIdOrderByUploadedAtDescIdDesc(userId).stream()
                .map(BankStatementMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BankStatementDetailDTO get(Long userId, Long id) {
        BankStatement statement = findOwned(userId, id);

        SortedMap<YearMonth, Long> counts = transactionRepository.findDatesByBankStatementId(id).stream()
                .collect(Collectors.groupingBy(date -> YearMonth.from(date), TreeMap::new, Collectors.counting()));
        List<MonthCountDTO> months = counts.entrySet().stream()
                .map(entry -> new MonthCountDTO(entry.getKey().toString(), entry.getValue()))
                .toList();

        return BankStatementMapper.toDetail(statement, months);
    }

    /** Undo an import. The database removes every transaction of the statement (ON DELETE CASCADE). */
    @Transactional
    public void delete(Long userId, Long id) {
        BankStatement statement = findOwned(userId, id);
        long removed = transactionRepository.countByBankStatementId(id);
        statementRepository.delete(statement);
        statementRepository.flush();
        log.info("User id={} deleted statement id={} and its {} transaction(s)", userId, id, removed);
    }

    // The user id is part of the query, so someone else's statement simply "does not exist" for you
    private BankStatement findOwned(Long userId, Long id) {
        return statementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank statement not found"));
    }
}