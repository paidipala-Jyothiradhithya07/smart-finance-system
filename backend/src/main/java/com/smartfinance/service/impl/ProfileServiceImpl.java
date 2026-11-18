package com.smartfinance.service.impl;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfinance.config.CurrencyProperties;
import com.smartfinance.dto.request.ProfileUpdateRequestDTO;
import com.smartfinance.dto.response.UserResponseDTO;
import com.smartfinance.entity.User;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.mapper.UserMapper;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.service.ProfileService;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final CurrencyProperties currencyProperties;

    public ProfileServiceImpl(UserRepository userRepository, CurrencyProperties currencyProperties) {
        this.userRepository = userRepository;
        this.currencyProperties = currencyProperties;
    }

    /** Changes only the name and the display currency. No amounts are converted. */
    @Override
    @Transactional
    public UserResponseDTO updateProfile(Long userId, ProfileUpdateRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String currency = request.currency().trim().toUpperCase(Locale.ROOT);
        if (!currencyProperties.isSupported(currency)) {
            throw new InvalidRequestException(
                    "Unsupported currency. Supported: " + currencyProperties.supportedCodes());
        }

        user.setName(request.name().trim());
        user.setCurrency(currency);
        user = userRepository.saveAndFlush(user);

        log.info("User id={} updated their profile", userId);
        return UserMapper.toResponse(user);
    }
}