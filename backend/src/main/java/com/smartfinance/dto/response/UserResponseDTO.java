package com.smartfinance.dto.response;

import java.time.LocalDateTime;

public record UserResponseDTO(
		Long id,
        String name,
        String email,
        String role,
        String currency,
        LocalDateTime createdAt
        ) {

}
