package com.smartfinance.dto.response;

public record LoginResponseDTO( 
		String token,
        String tokenType,
        long expiresInSeconds,
        UserResponseDTO user) {

}
