package com.smartfinance.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartfinance.config.StatementProperties;
import com.smartfinance.dto.response.DuplicateRowDTO;
import com.smartfinance.dto.response.ImportFileResultDTO;
import com.smartfinance.dto.response.ImportResponse;
import com.smartfinance.dto.response.ImportStatus;
import com.smartfinance.dto.response.MonthCountDTO;
import com.smartfinance.dto.response.RowErrorDTO;
import com.smartfinance.entity.BankStatement;
import com.smartfinance.exception.FileProcessingException;
import com.smartfinance.exception.InvalidFileException;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.repository.BankStatementRepository;
import com.smartfinance.statement.StatementFileValidator;
import com.smartfinance.statement.StatementImporter;
import com.smartfinance.statement.StatementParser;
import com.smartfinance.statement.ValidatedFile;
import com.smartfinance.statement.dedup.Fingerprints;
import com.smartfinance.statement.model.ParsedStatement;
import com.smartfinance.statement.model.ParsedTransaction;

@Service
public class StatementImportService {

    private static final Logger log = LoggerFactory.getLogger(StatementImportService.class);
    private static final int MAX_ERRORS_SHOWN = 100;
    private static final int MAX_DUPLICATES_SHOWN = 50;

    private record FileImport(ImportFileResultDTO result, Set<YearMonth> importedMonths) {
        static FileImport of(ImportFileResultDTO result) {
            return new FileImport(result, Set.of());
        }
    }

    private final StatementFileValidator validator;
    private final StatementParser parser;
    private final StatementImporter importer;
    private final BankStatementRepository statementRepository;
    private final StatementProperties properties;

    public StatementImportService(StatementFileValidator validator, StatementParser parser,
                                  StatementImporter importer, BankStatementRepository statementRepository,
                                  StatementProperties properties) {
        this.validator = validator;
        this.parser = parser;
        this.importer = importer;
        this.statementRepository = statementRepository;
        this.properties = properties;
    }

    public ImportResponse importFiles(Long userId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidRequestException("Please attach at least one file");
        }
        if (files.size() > properties.getMaxFilesPerUpload()) {
            throw new InvalidRequestException(
                    "At most " + properties.getMaxFilesPerUpload() + " files can be uploaded at once");
        }

        List<ImportFileResultDTO> results = new ArrayList<>();
        Set<YearMonth> months = new TreeSet<>();
        for (MultipartFile file : files) {
            FileImport fileImport = importOne(userId, file);
            results.add(fileImport.result());
            months.addAll(fileImport.importedMonths());
        }

        ImportResponse response = summarize(files.size(), results, months.size());
        // Counts only. Statement contents are private and never written to the log.
        log.info("User id={} uploaded {} file(s): {} imported, {} transaction(s) saved",
                userId, files.size(), response.filesImported(), response.transactionsImported());
        return response;
    }

    // One bad file does not stop the others, and each file is saved in its own database transaction
    private FileImport importOne(Long userId, MultipartFile file) {
        String displayName = StatementFileValidator.safeFileName(file.getOriginalFilename());
        try {
            ValidatedFile validated = validator.validate(file);
            String fileHash = Fingerprints.fileHash(validated.content());

            Optional<BankStatement> earlier = statementRepository.findByUserIdAndFileHash(userId, fileHash);
            if (earlier.isPresent()) {
                return FileImport.of(alreadyImported(displayName, earlier.get()));
            }

            ParsedStatement parsed = parser.parse(validated.fileName(), validated.type(), validated.content());
            if (parsed.transactions().isEmpty()) {
                return FileImport.of(build(parsed, ImportStatus.FAILED,
                        "No valid transactions were found in this file, so nothing was imported.",
                        null, 0, List.of(), parsed.warnings()));
            }

            return toResult(parsed, importer.save(userId, fileHash, parsed));
        } catch (InvalidFileException | FileProcessingException ex) {
            return FileImport.of(ImportFileResultDTO.failed(displayName, ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            // The database rolled the whole file back. This happens when the same data is imported twice at once.
            log.warn("Import conflict for user id={}", userId);
            return FileImport.of(ImportFileResultDTO.failed(displayName,
                    "This statement conflicted with data being imported at the same time, so nothing was saved. "
                    + "Check your statement list and try again if needed."));
        }
    }

    private FileImport toResult(ParsedStatement parsed, StatementImporter.Outcome outcome) {
        List<String> warnings = new ArrayList<>(parsed.warnings());
        warnings.addAll(outcome.balanceWarnings());

        if (outcome.statement() == null) {
            int count = parsed.transactions().size();
            String message = "All " + plural(count, "valid transaction") + " in this file "
                    + (count == 1 ? "was" : "were") + " already imported.";
            return FileImport.of(build(parsed, ImportStatus.NO_NEW_TRANSACTIONS, message, null, 0,
                    outcome.duplicates(), warnings));
        }

        int other = outcome.uncategorized();
        if (other > 0) {
            warnings.add(plural(other, "transaction") + " could not be categorized automatically and "
                    + (other == 1 ? "was" : "were") + " set to 'Other'. You can change the category later.");
        }

        ImportStatus status = parsed.invalidRows() > 0
                ? ImportStatus.IMPORTED_WITH_ERRORS : ImportStatus.IMPORTED;
        ImportFileResultDTO result = build(parsed, status,
                describe(outcome.imported(), outcome.duplicates().size(), parsed.invalidRows()),
                outcome.statement().getId(), outcome.imported(), outcome.duplicates(), warnings);
        return new FileImport(result, outcome.importedMonths());
    }

    private ImportFileResultDTO alreadyImported(String fileName, BankStatement earlier) {
        String message = "This file was already imported on " + earlier.getUploadedAt().toLocalDate()
                + ". Delete that statement first if you want to import it again.";
        return new ImportFileResultDTO(fileName, ImportStatus.ALREADY_IMPORTED, message, earlier.getId(),
                0, 0, 0, 0, 0, earlier.getPeriodStart(), earlier.getPeriodEnd(),
                List.of(), List.of(), List.of(), false, List.of(), false);
    }

    private ImportFileResultDTO build(ParsedStatement parsed, ImportStatus status, String message,
                                      Long statementId, int imported, List<ParsedTransaction> duplicates,
                                      List<String> warnings) {
        List<RowErrorDTO> errors = parsed.errors().stream()
                .limit(MAX_ERRORS_SHOWN)
                .map(e -> new RowErrorDTO(e.rowNumber(), e.column(), e.message()))
                .toList();
        List<DuplicateRowDTO> duplicateRows = duplicates.stream()
                .limit(MAX_DUPLICATES_SHOWN)
                .map(t -> new DuplicateRowDTO(t.rowNumber(), t.date(), t.description(), t.amount(), t.type()))
                .toList();
        List<MonthCountDTO> months = parsed.monthCounts().entrySet().stream()
                .map(entry -> new MonthCountDTO(entry.getKey().toString(), entry.getValue()))
                .toList();

        return new ImportFileResultDTO(
                parsed.fileName(), status, message, statementId,
                parsed.transactions().size() + parsed.invalidRows(),
                imported, duplicates.size(), parsed.invalidRows(), parsed.skippedRows(),
                parsed.periodStart(), parsed.periodEnd(), months, warnings,
                errors, parsed.errors().size() > MAX_ERRORS_SHOWN,
                duplicateRows, duplicates.size() > MAX_DUPLICATES_SHOWN);
    }

    private ImportResponse summarize(int received, List<ImportFileResultDTO> results, int monthsImported) {
        int filesImported = 0;
        int transactionsImported = 0;
        int duplicates = 0;
        int invalid = 0;
        for (ImportFileResultDTO result : results) {
            duplicates += result.duplicates();
            invalid += result.invalid();
            if (result.status() == ImportStatus.IMPORTED || result.status() == ImportStatus.IMPORTED_WITH_ERRORS) {
                filesImported++;
                transactionsImported += result.imported();
            }
        }
        String message = monthsImported > 0
                ? plural(monthsImported, "month") + " of financial data successfully imported."
                : "No new transactions were imported.";
        return new ImportResponse(received, filesImported, transactionsImported, duplicates, invalid,
                monthsImported, message, results);
    }

    private static String describe(int imported, int duplicates, int invalid) {
        StringBuilder text = new StringBuilder("Imported " + plural(imported, "transaction") + ".");
        if (duplicates > 0) {
            text.append(' ').append(plural(duplicates, "duplicate"))
                    .append(duplicates == 1 ? " was" : " were").append(" skipped.");
        }
        if (invalid > 0) {
            text.append(' ').append(plural(invalid, "row"))
                    .append(invalid == 1 ? " had errors and was" : " had errors and were")
                    .append(" not imported.");
        }
        return text.toString();
    }

    private static String plural(int count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }
}