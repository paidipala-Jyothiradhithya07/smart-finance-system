package com.smartfinance.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartfinance.dto.response.StatementPreviewResponse;
import com.smartfinance.security.AuthenticatedUser;
import com.smartfinance.service.StatementPreviewService;

@RestController
@RequestMapping("/api/bank-statements")
public class BankStatementController {
	private final StatementPreviewService previewService;

    public BankStatementController(StatementPreviewService previewService) {
        this.previewService = previewService;
    }

    // Dry run: reads and checks the file(s), saves nothing
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StatementPreviewResponse preview(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @RequestParam("files") List<MultipartFile> files) {
        return previewService.preview(principal.getId(), files);
    }
}
