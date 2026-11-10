package com.smartfinance.service;


import com.smartfinance.dto.request.TransactionRequestDTO;
import com.smartfinance.dto.response.PageResponseDTO;
import com.smartfinance.dto.response.TransactionResponseDTO;

public interface TransactionService {
	 TransactionResponseDTO create(Long userId, TransactionRequestDTO request);

	    TransactionResponseDTO getById(Long userId, Long id);

	    PageResponseDTO<TransactionResponseDTO> list(Long userId, int page, int size,
	                                                 String sortBy, String direction);

	    TransactionResponseDTO update(Long userId, Long id, TransactionRequestDTO request);

	    void delete(Long userId, Long id);
}
