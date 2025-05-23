package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.JwtResponse;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.SignupRequest;
import com.ecommerce.service.interfaces.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
// Using fully qualified name for Swagger ApiResponse to avoid collision with DTO ApiResponse
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API xác thực người dùng")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "Đăng nhập", description = "Xác thực người dùng và trả về token JWT")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đăng nhập thành công",
                content = @Content(schema = @Schema(implementation = JwtResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Thông tin đăng nhập không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content)
    })
    @PostMapping("/signin")
    @RateLimit(anonymousLimit = 5, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(
            @Parameter(description = "Thông tin đăng nhập", required = true) @Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        ApiResponse<JwtResponse> response = ApiResponse.success("Login successful", jwtResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đăng ký", description = "Đăng ký tài khoản người dùng mới")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đăng ký thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc tên đăng nhập/email đã tồn tại",
                content = @Content)
    })
    @PostMapping("/signup")
    @RateLimit(anonymousLimit = 3, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> registerUser(
            @Parameter(description = "Thông tin đăng ký", required = true) @Valid @RequestBody SignupRequest signUpRequest) {
        authService.registerUser(signUpRequest);
        ApiResponse<JwtResponse> response = ApiResponse.success("User registered successfully!", null);
        return ResponseEntity.ok(response);       
    }
}