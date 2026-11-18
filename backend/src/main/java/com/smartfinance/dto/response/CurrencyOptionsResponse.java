package com.smartfinance.dto.response;

import java.util.List;

public record CurrencyOptionsResponse(
		String defaultCurrency,
		List<CurrencyDTO> currencies
		) {
}