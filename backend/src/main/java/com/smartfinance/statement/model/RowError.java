package com.smartfinance.statement.model;

public record RowError(
		int rowNumber, 
		String column,
		String message) {
}
