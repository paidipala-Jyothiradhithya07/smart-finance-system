package com.smartfinance.controller;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartfinance.dto.request.LoginRequestDTO;
import com.smartfinance.dto.request.UserRequestDTO;
import com.smartfinance.dto.response.LoginResponseDTO;
import com.smartfinance.dto.response.UserResponseDTO;
import com.smartfinance.security.AuthenticatedUser;
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
	        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	    }

	    @PostMapping("/login")
	    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
	        return authService.login(request);
	    }

	    // The user id comes from the verified token, never from anything the client types
	    @GetMapping("/me")
	    public UserResponseDTO me(@AuthenticationPrincipal AuthenticatedUser principal) {
	        return authService.getCurrentUser(principal.getId());
	    }

	    // JWT is stateless: the server keeps no session, so the client logs out by deleting its token.
	    // This endpoint exists so the frontend has one place to call; real revocation is an advanced feature.
	    @PostMapping("/logout")
	    @ResponseStatus(HttpStatus.NO_CONTENT)
	    public void logout() {
	    }
}

