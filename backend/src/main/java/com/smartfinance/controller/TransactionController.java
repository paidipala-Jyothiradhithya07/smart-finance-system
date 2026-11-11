 package com.smartfinance.controller;

 import java.math.BigDecimal;
 import java.time.LocalDate;

 import org.springframework.format.annotation.DateTimeFormat;
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

 import com.smartfinance.dto.request.TransactionFilter;
 import com.smartfinance.dto.request.TransactionRequestDTO;
 import com.smartfinance.dto.response.PageResponseDTO;
 import com.smartfinance.dto.response.TransactionResponseDTO;
 import com.smartfinance.entity.enums.TransactionSource;
 import com.smartfinance.entity.enums.TransactionType;
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

	    // Income page: ?type=CREDIT    Expenses page: ?type=DEBIT
	    @GetMapping
	    public PageResponseDTO<TransactionResponseDTO> search(
	            @AuthenticationPrincipal AuthenticatedUser principal,
	            @RequestParam(name = "from", required = false)
	                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
	            @RequestParam(name = "to", required = false)
	                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
	            @RequestParam(name = "categoryId", required = false) Long categoryId,
	            @RequestParam(name = "type", required = false) TransactionType type,
	            @RequestParam(name = "minAmount", required = false) BigDecimal minAmount,
	            @RequestParam(name = "maxAmount", required = false) BigDecimal maxAmount,
	            @RequestParam(name = "q", required = false) String keyword,
	            @RequestParam(name = "source", required = false) TransactionSource source,
	            @RequestParam(name = "page", defaultValue = "0") int page,
	            @RequestParam(name = "size", defaultValue = "20") int size,
	            @RequestParam(name = "sortBy", defaultValue = "transactionDate") String sortBy,
	            @RequestParam(name = "direction", defaultValue = "desc") String direction) {

	        TransactionFilter filter = new TransactionFilter(
	                from, to, categoryId, type, minAmount, maxAmount, keyword, source);
	        return transactionService.search(principal.getId(), filter, page, size, sortBy, direction);
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
