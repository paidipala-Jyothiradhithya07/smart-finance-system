package com.smartfinance.mapper;

import com.smartfinance.dto.response.UserResponseDTO;
import com.smartfinance.entity.User;

public class UserMapper {
	  private UserMapper() {
	    }

	    public static UserResponseDTO toResponse(User user) {
	        return new UserResponseDTO(
	                user.getId(),
	                user.getName(),
	                user.getEmail(),
	                user.getRole().name(),
	                user.getCurrency(),
	                user.getCreatedAt());
	    }
}
