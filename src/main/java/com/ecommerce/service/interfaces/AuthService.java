package com.ecommerce.service.interfaces;

import com.ecommerce.dto.JwtResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.SignupRequest;

public interface AuthService {
    
    JwtResponse authenticateUser(LoginRequest loginRequest);
    
    void registerUser(SignupRequest signUpRequest);
} 