package com.smartfinance.service.impl;


import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfinance.config.CurrencyProperties;
import com.smartfinance.dto.request.LoginRequestDTO;
import com.smartfinance.dto.request.UserRequestDTO;
import com.smartfinance.dto.response.LoginResponseDTO;
import com.smartfinance.dto.response.UserResponseDTO;
import com.smartfinance.entity.User;
import com.smartfinance.entity.enums.Role;
import com.smartfinance.exception.DuplicateEmailException;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.mapper.UserMapper;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.security.AuthenticatedUser;
import com.smartfinance.security.JwtService;
import com.smartfinance.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

	 private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	    private final UserRepository userRepository;
	    private final PasswordEncoder passwordEncoder;
	    private final CurrencyProperties currencyProperties;
	    private final AuthenticationManager authenticationManager;
	    private final JwtService jwtService;

	    public AuthServiceImpl(UserRepository userRepository,
	                           PasswordEncoder passwordEncoder,
	                           CurrencyProperties currencyProperties,
	                           AuthenticationManager authenticationManager,
	                           JwtService jwtService) {
	        this.userRepository = userRepository;
	        this.passwordEncoder = passwordEncoder;
	        this.currencyProperties = currencyProperties;
	        this.authenticationManager = authenticationManager;
	        this.jwtService = jwtService;
	    }

	    @Override
	    @Transactional
	    public UserResponseDTO register(UserRequestDTO request) {
	        String email = normalizeEmail(request.email());

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
	            throw new DuplicateEmailException("An account with this email already exists");
	        }

	        log.info("Registered new user with id={}", user.getId());
	        return UserMapper.toResponse(user);
	    }

	    @Override
	    @Transactional(readOnly = true)
	    public LoginResponseDTO login(LoginRequestDTO request) {
	        // Throws BadCredentialsException for a wrong password OR an unknown email (same error on purpose)
	        Authentication authentication = authenticationManager.authenticate(
	                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));

	        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
	        User user = userRepository.findById(principal.getId())
	                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

	        String token = jwtService.generateToken(principal);
	        log.info("User id={} logged in", user.getId());

	        return new LoginResponseDTO(token, "Bearer", jwtService.getExpirationSeconds(),
	                UserMapper.toResponse(user));
	    }

	    @Override
	    @Transactional(readOnly = true)
	    public UserResponseDTO getCurrentUser(Long userId) {
	        User user = userRepository.findById(userId)
	                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
	        return UserMapper.toResponse(user);
	    }

	    private String normalizeEmail(String email) {
	        return email.trim().toLowerCase(Locale.ROOT);
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
