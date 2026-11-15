package com.smartfinance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartfinance.dto.response.BankStatementDetailDTO;
import com.smartfinance.dto.response.BankStatementResponseDTO;
import com.smartfinance.dto.response.ImportResponse;
import com.smartfinance.dto.response.StatementPreviewResponse;
import com.smartfinance.security.AuthenticatedUser;
import com.smartfinance.service.BankStatementService;
import com.smartfinance.service.StatementImportService;
import com.smartfinance.service.StatementPreviewService;

@RestController
@RequestMapping("/api/bank-statements")
public class BankStatementController {

    private final StatementPreviewService previewService;
    private final StatementImportService importService;
    private final BankStatementService statementService;

    public BankStatementController(StatementPreviewService previewService,
                                   StatementImportService importService,
                                   BankStatementService statementService) {
        this.previewService = previewService;
        this.importService = importService;
        this.statementService = statementService;
    }

    // Dry run: reads and checks the file(s), saves nothing
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StatementPreviewResponse preview(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @RequestParam("files") List<MultipartFile> files) {
        return previewService.preview(principal.getId(), files);
    }

    // The real import. 201 when at least one statement was created, otherwise 200 with the report.
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResponse> upload(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @RequestParam("files") List<MultipartFile> files) {
        ImportResponse response = importService.importFiles(principal.getId(), files);
        HttpStatus status = response.filesImported() > 0 ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    public List<BankStatementResponseDTO> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return statementService.list(principal.getId());
    }

    @GetMapping("/{id}")
    public BankStatementDetailDTO get(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable Long id) {
        return statementService.get(principal.getId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        statementService.delete(principal.getId(), id);
    }
}