package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.JwtResponse;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.SignupRequest;
import com.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    @RateLimit(anonymousLimit = 5, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        ApiResponse<JwtResponse> response = ApiResponse.success("Login successful", jwtResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @RateLimit(anonymousLimit = 3, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        authService.registerUser(signUpRequest);
        ApiResponse<JwtResponse> response = ApiResponse.success("User registered successfully!", null);
        return ResponseEntity.ok(response);       
    }
}