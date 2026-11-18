package com.smartfinance.dto.response;

import java.util.List;

public record MonthlySummaryResponse(String currency, 
		String from, String to,
		List<MonthlySummaryDTO> months) {
}