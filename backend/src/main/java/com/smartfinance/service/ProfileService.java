package com.smartfinance.service;

import com.smartfinance.dto.request.ProfileUpdateRequestDTO;
import com.smartfinance.dto.response.UserResponseDTO;

public interface ProfileService {

    UserResponseDTO updateProfile(Long userId, ProfileUpdateRequestDTO request);
}