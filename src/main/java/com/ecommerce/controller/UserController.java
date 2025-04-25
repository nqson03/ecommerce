package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.DeleteAccountRequest;
import com.ecommerce.dto.PasswordChangeRequest;
import com.ecommerce.dto.UserDto;
import com.ecommerce.model.User;
import com.ecommerce.service.UserService;
import com.ecommerce.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        long id = customUserDetails.getId(); // Get the user's ID from the token payload
        return ResponseEntity.ok(ApiResponse.success("User information retrieved successfully", userService.getCurrentUserInfo(id)));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<UserDto>> updateCurrentUser(@Valid @RequestBody UserDto userDto) {
        return ResponseEntity.ok(ApiResponse.success("User information updated successfully", userService.updateCurrentUser(userDto)));
    }

    @PutMapping("/me/password")
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 5, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> changePassword(@AuthenticationPrincipal CustomUserDetails customUserDetails, @Valid @RequestBody PasswordChangeRequest passwordChangeRequest) {
        long id = customUserDetails.getId(); // Get the user's ID from the token payload
        userService.changePassword(id, passwordChangeRequest.getCurrentPassword(), passwordChangeRequest.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 3, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> deleteAccount(@AuthenticationPrincipal CustomUserDetails customUserDetails, @Valid @RequestBody DeleteAccountRequest deleteAccountRequest) {
        long id = customUserDetails.getId(); // Get the user's ID from the token payload
        userService.deleteCurrentUser(id, deleteAccountRequest.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }

    // Admin endpoints
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", userService.getAllUsers(pageable)));
    }

    @PutMapping("/admin/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
            @PathVariable Long id, @RequestParam User.Role role) {
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", userService.updateUserRole(id, role)));
    }
}