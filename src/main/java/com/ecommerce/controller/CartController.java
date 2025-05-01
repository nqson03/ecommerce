package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.service.CartService;
import com.ecommerce.service.UserService; 
import com.ecommerce.model.User; 
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
// Using fully qualified name for Swagger ApiResponse to avoid collision with DTO ApiResponse
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('USER')")
@Tag(name = "Cart", description = "API quản lý giỏ hàng")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService; 

    @Operation(summary = "Lấy thông tin giỏ hàng", description = "Trả về thông tin giỏ hàng của người dùng hiện tại", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin giỏ hàng thành công",
                content = @Content(schema = @Schema(implementation = CartDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @GetMapping
    @RateLimit(authenticatedLimit = 100, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> getCart() {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.getCart(currentUser); 
        ApiResponse<CartDto> response = ApiResponse.success("Get cart successfully", cartDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Thêm sản phẩm vào giỏ hàng", description = "Thêm sản phẩm mới vào giỏ hàng của người dùng hiện tại", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thêm sản phẩm vào giỏ hàng thành công",
                content = @Content(schema = @Schema(implementation = CartDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm",
                content = @Content)
    })
    @PostMapping("/items")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> addItemToCart(
            @Parameter(description = "Thông tin sản phẩm thêm vào giỏ hàng", required = true) @Valid @RequestBody CartItemRequest request) {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.addItemToCart(currentUser, request); 
        ApiResponse<CartDto> response = ApiResponse.success("Add item to cart successfully", cartDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cập nhật sản phẩm trong giỏ hàng", description = "Cập nhật số lượng sản phẩm trong giỏ hàng của người dùng hiện tại", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật sản phẩm trong giỏ hàng thành công",
                content = @Content(schema = @Schema(implementation = CartDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm trong giỏ hàng",
                content = @Content)
    })
    @PutMapping("/items/{itemId}")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> updateCartItem(
            @Parameter(description = "ID của sản phẩm trong giỏ hàng", required = true) @PathVariable Long itemId, 
            @Parameter(description = "Thông tin cập nhật sản phẩm", required = true) @Valid @RequestBody CartItemRequest request) {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.updateCartItem(currentUser, itemId, request); 
        ApiResponse<CartDto> response = ApiResponse.success("Update cart item successfully", cartDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng", description = "Xóa sản phẩm khỏi giỏ hàng của người dùng hiện tại", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa sản phẩm khỏi giỏ hàng thành công",
                content = @Content(schema = @Schema(implementation = CartDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm trong giỏ hàng",
                content = @Content)
    })
    @DeleteMapping("/items/{itemId}")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> removeItemFromCart(
            @Parameter(description = "ID của sản phẩm trong giỏ hàng", required = true) @PathVariable Long itemId) {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.removeItemFromCart(currentUser, itemId); 
        ApiResponse<CartDto> response = ApiResponse.success("Remove item from cart successfully", cartDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa toàn bộ giỏ hàng", description = "Xóa tất cả sản phẩm trong giỏ hàng của người dùng hiện tại", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa giỏ hàng thành công",
                content = @Content(schema = @Schema(implementation = CartDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @DeleteMapping("/clear")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> clearCart() {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.clearCart(currentUser); 
        ApiResponse<CartDto> response = ApiResponse.success("Clear cart successfully", cartDto);
        return ResponseEntity.ok(response);
    }
}