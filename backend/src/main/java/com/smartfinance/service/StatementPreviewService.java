package com.smartfinance.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartfinance.config.StatementProperties;
import com.smartfinance.dto.response.FilePreviewDTO;
import com.smartfinance.dto.response.MonthCountDTO;
import com.smartfinance.dto.response.PreviewTransactionDTO;
import com.smartfinance.dto.response.RowErrorDTO;
import com.smartfinance.dto.response.StatementPreviewResponse;
import com.smartfinance.exception.FileProcessingException;
import com.smartfinance.exception.InvalidFileException;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.statement.StatementFileValidator;
import com.smartfinance.statement.StatementParser;
import com.smartfinance.statement.ValidatedFile;
import com.smartfinance.statement.model.ParsedStatement;

/** Reads and checks statements and reports what would be imported. Saves nothing. */
@Service
public class StatementPreviewService {
	private static final Logger log = LoggerFactory.getLogger(StatementPreviewService.class);
    private static final int MAX_ERRORS_SHOWN = 100;
    private static final int SAMPLE_SIZE = 5;

    private final StatementFileValidator validator;
    private final StatementParser parser;
    private final StatementProperties properties;

    public StatementPreviewService(StatementFileValidator validator, StatementParser parser,
                                   StatementProperties properties) {
        this.validator = validator;
        this.parser = parser;
        this.properties = properties;
    }

    public StatementPreviewResponse preview(Long userId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidRequestException("Please attach at least one file");
        }
        if (files.size() > properties.getMaxFilesPerUpload()) {
            throw new InvalidRequestException(
                    "At most " + properties.getMaxFilesPerUpload() + " files can be uploaded at once");
        }

        List<FilePreviewDTO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(previewOne(file));
        }
        int readable = (int) results.stream().filter(result -> "OK".equals(result.status())).count();
        log.info("User id={} previewed {} file(s), {} readable", userId, files.size(), readable);
        return new StatementPreviewResponse(files.size(), readable, results);
    }

    // One bad file does not stop the others from being read
    private FilePreviewDTO previewOne(MultipartFile file) {
        String displayName = StatementFileValidator.safeFileName(file.getOriginalFilename());
        try {
            ValidatedFile validated = validator.validate(file);
            ParsedStatement parsed = parser.parse(validated.fileName(), validated.type(), validated.content());
            return toDto(parsed);
        } catch (InvalidFileException | FileProcessingException ex) {
            return FilePreviewDTO.failed(displayName, ex.getMessage());
        }
    }

    private FilePreviewDTO toDto(ParsedStatement parsed) {
        List<RowErrorDTO> errors = parsed.errors().stream()
                .limit(MAX_ERRORS_SHOWN)
                .map(e -> new RowErrorDTO(e.rowNumber(), e.column(), e.message()))
                .toList();

        List<MonthCountDTO> months = parsed.monthCounts().entrySet().stream()
                .map(entry -> new MonthCountDTO(entry.getKey().toString(), entry.getValue()))
                .toList();

        List<PreviewTransactionDTO> sample = parsed.transactions().stream()
                .limit(SAMPLE_SIZE)
                .map(t -> new PreviewTransactionDTO(t.rowNumber(), t.date(), t.description(),
                        t.amount(), t.type(), t.balance(), t.reference()))
                .toList();

        return new FilePreviewDTO(
                parsed.fileName(),
                "OK",
                null,
                parsed.fileType().name(),
                parsed.headerRowNumber(),
                parsed.mapping().describe(),
                parsed.totalDataRows(),
                parsed.transactions().size(),
                parsed.invalidRows(),
                parsed.skippedRows(),
                parsed.periodStart(),
                parsed.periodEnd(),
                months,
                parsed.warnings(),
                errors,
                parsed.errors().size() > MAX_ERRORS_SHOWN,
                sample);
    }
}

