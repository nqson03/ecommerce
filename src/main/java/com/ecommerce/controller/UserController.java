package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.DeleteAccountRequest;
import com.ecommerce.dto.PasswordChangeRequest;
import com.ecommerce.dto.UserDto;
import com.ecommerce.model.User;
import com.ecommerce.service.interfaces.UserService;
import com.ecommerce.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "API quản lý người dùng")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Lấy thông tin người dùng hiện tại", description = "Trả về thông tin chi tiết của người dùng đang đăng nhập", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin người dùng thành công",
                content = @Content(schema = @Schema(implementation = UserDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        long id = customUserDetails.getId(); // Get the user's ID from the token payload
        return ResponseEntity.ok(ApiResponse.success("User information retrieved successfully", userService.getCurrentUserInfo(id)));
    }

    @Operation(summary = "Cập nhật thông tin người dùng", description = "Cập nhật thông tin của người dùng đang đăng nhập", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thông tin người dùng thành công",
                content = @Content(schema = @Schema(implementation = UserDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<UserDto>> updateCurrentUser(
            @Parameter(description = "Thông tin người dùng cập nhật", required = true) @Valid @RequestBody UserDto userDto) {
        return ResponseEntity.ok(ApiResponse.success("User information updated successfully", userService.updateCurrentUser(userDto)));
    }

    @Operation(summary = "Đổi mật khẩu", description = "Thay đổi mật khẩu của người dùng đang đăng nhập", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc mật khẩu hiện tại không đúng",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @PutMapping("/me/password")
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 5, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails, 
            @Parameter(description = "Thông tin đổi mật khẩu", required = true) @Valid @RequestBody PasswordChangeRequest passwordChangeRequest) {
        long id = customUserDetails.getId(); // Get the user's ID from the token payload
        userService.changePassword(id, passwordChangeRequest.getCurrentPassword(), passwordChangeRequest.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @Operation(summary = "Xóa tài khoản", description = "Xóa tài khoản của người dùng đang đăng nhập", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa tài khoản thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc mật khẩu không đúng",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @DeleteMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 3, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> deleteAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails, 
            @Parameter(description = "Thông tin xác nhận xóa tài khoản", required = true) @Valid @RequestBody DeleteAccountRequest deleteAccountRequest) {
        long id = customUserDetails.getId(); // Get the user's ID from the token payload
        userService.deleteCurrentUser(id, deleteAccountRequest.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }

    // Admin endpoints
    @Operation(summary = "Lấy danh sách tất cả người dùng (Admin)", description = "Trả về danh sách tất cả người dùng trong hệ thống", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách người dùng thành công",
                content = @Content(schema = @Schema(implementation = Page.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @Parameter(description = "Thông tin phân trang") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", userService.getAllUsers(pageable)));
    }

    @Operation(summary = "Cập nhật vai trò người dùng (Admin)", description = "Cập nhật vai trò của người dùng trong hệ thống", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật vai trò người dùng thành công",
                content = @Content(schema = @Schema(implementation = UserDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng",
                content = @Content)
    })
    @PutMapping("/admin/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
            @Parameter(description = "ID của người dùng", required = true) @PathVariable Long id, 
            @Parameter(description = "Vai trò mới của người dùng", required = true) @RequestParam User.Role role) {
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", userService.updateUserRole(id, role)));
    }
}