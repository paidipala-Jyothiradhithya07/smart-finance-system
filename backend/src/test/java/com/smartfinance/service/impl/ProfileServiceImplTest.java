package com.smartfinance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartfinance.config.CurrencyProperties;
import com.smartfinance.dto.request.ProfileUpdateRequestDTO;
import com.smartfinance.dto.response.UserResponseDTO;
import com.smartfinance.entity.User;
import com.smartfinance.exception.InvalidRequestException;
import com.smartfinance.exception.ResourceNotFoundException;
import com.smartfinance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        CurrencyProperties properties = new CurrencyProperties();
        properties.setDefault("INR");
        properties.setSupported(List.of(item("INR"), item("USD"), item("GBP")));
        service = new ProfileServiceImpl(userRepository, properties);
    }

    @Test
    void updatesTheNameAndCurrency() {
        User user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO response = service.updateProfile(1L, new ProfileUpdateRequestDTO("  Asha Rao  ", "usd"));

        assertThat(response.name()).isEqualTo("Asha Rao");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(user.getName()).isEqualTo("Asha Rao");
    }

    @Test
    void rejectsAnUnsupportedCurrency() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> service.updateProfile(1L, new ProfileUpdateRequestDTO("Asha", "EUR")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported currency");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void unknownUser_isNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(2L, new ProfileUpdateRequestDTO("Asha", "INR")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static CurrencyProperties.Item item(String code) {
        CurrencyProperties.Item item = new CurrencyProperties.Item();
        item.setCode(code);
        item.setSymbol(code);
        item.setName(code);
        return item;
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setName("Old Name");
        user.setEmail("asha@example.com");
        user.setCurrency("INR");
        return user;
    }
}