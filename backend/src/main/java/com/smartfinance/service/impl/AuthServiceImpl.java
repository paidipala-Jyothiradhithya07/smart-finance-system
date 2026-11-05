package com.smartfinance.service.impl;


import java.util.Locale;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartfinance.config.CurrencyProperties;
import com.smartfinance.dto.request.UserRequestDTO;
import com.smartfinance.dto.response.UserResponseDTO;
import com.smartfinance.entity.User;
import com.smartfinance.entity.enums.Role;
import com.smartfinance.exception.DuplicateEmailException;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.mapper.UserMapper;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.service.AuthService;

import jakarta.transaction.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

	 private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	    private final UserRepository userRepository;
	    private final PasswordEncoder passwordEncoder;
	    private final CurrencyProperties currencyProperties;

	    public AuthServiceImpl(UserRepository userRepository,
	                           PasswordEncoder passwordEncoder,
	                           CurrencyProperties currencyProperties) {
	        this.userRepository = userRepository;
	        this.passwordEncoder = passwordEncoder;
	        this.currencyProperties = currencyProperties;
	    }

	    @Override
	    @Transactional
	    public UserResponseDTO register(UserRequestDTO request) {
	        String email = request.email().trim().toLowerCase(Locale.ROOT);

	        if (userRepository.existsByEmail(email)) {
	            throw new DuplicateEmailException("An account with this email already exists");
	        }

	        User user = new User();
	        user.setName(request.name().trim());
	        user.setEmail(email);
	        user.setPasswordHash(passwordEncoder.encode(request.password()));
	        user.setRole(Role.USER);
	        user.setCurrency(resolveCurrency(request.currency()));

	        try {
	            user = userRepository.saveAndFlush(user);
	        } catch (DataIntegrityViolationException ex) {
	            // Two people registering the same email at the same moment: the database unique key stops one
	            throw new DuplicateEmailException("An account with this email already exists");
	        }

	        log.info("Registered new user with id={}", user.getId());
	        return UserMapper.toResponse(user);
	    }

	    private String resolveCurrency(String requested) {
	        if (requested == null || requested.isBlank()) {
	            return currencyProperties.getDefault();
	        }
	        String code = requested.trim().toUpperCase(Locale.ROOT);
	        if (!currencyProperties.isSupported(code)) {
	            throw new InvalidRequestException(
	                    "Unsupported currency. Supported: " + currencyProperties.supportedCodes());
	        }
	        return code;
	    }

}
