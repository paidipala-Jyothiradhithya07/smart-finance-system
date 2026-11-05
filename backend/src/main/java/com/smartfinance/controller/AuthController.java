package com.smartfinance.controller;


import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartfinance.dto.request.UserRequestDTO;
import com.smartfinance.dto.response.UserResponseDTO;
import com.smartfinance.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO request) {
        UserResponseDTO created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

