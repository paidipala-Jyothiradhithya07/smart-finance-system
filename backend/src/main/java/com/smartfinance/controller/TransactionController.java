 package com.smartfinance.controller;

 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.security.core.annotation.AuthenticationPrincipal;
 import org.springframework.web.bind.annotation.DeleteMapping;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.PutMapping;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.ResponseStatus;
 import org.springframework.web.bind.annotation.RestController;

 import com.smartfinance.dto.request.TransactionRequestDTO;
 import com.smartfinance.dto.response.PageResponseDTO;
 import com.smartfinance.dto.response.TransactionResponseDTO;
 import com.smartfinance.security.AuthenticatedUser;
 import com.smartfinance.service.TransactionService;

 import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TransactionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(principal.getId(), request));
    }

    @GetMapping
    public PageResponseDTO<TransactionResponseDTO> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return transactionService.list(principal.getId(), page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public TransactionResponseDTO getById(@AuthenticationPrincipal AuthenticatedUser principal,
                                          @PathVariable Long id) {
        return transactionService.getById(principal.getId(), id);
    }

    @PutMapping("/{id}")
    public TransactionResponseDTO update(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable Long id,
                                         @Valid @RequestBody TransactionRequestDTO request) {
        return transactionService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        transactionService.delete(principal.getId(), id);
    }
}
