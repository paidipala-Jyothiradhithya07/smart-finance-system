package com.smartfinance.service;

import com.smartfinance.dto.request.UserRequestDTO;
import com.smartfinance.dto.response.UserResponseDTO;

public interface AuthService {
	UserResponseDTO register(UserRequestDTO request);
}
