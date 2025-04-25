package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.service.CartService;
import com.ecommerce.service.UserService; 
import com.ecommerce.model.User; 
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('USER')")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService; 

    @GetMapping
    @RateLimit(authenticatedLimit = 100, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> getCart() {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.getCart(currentUser); 
        ApiResponse<CartDto> response = ApiResponse.success("Get cart successfully", cartDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> addItemToCart(@Valid @RequestBody CartItemRequest request) {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.addItemToCart(currentUser, request); 
        ApiResponse<CartDto> response = ApiResponse.success("Add item to cart successfully", cartDto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{itemId}")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> updateCartItem(
            @PathVariable Long itemId, @Valid @RequestBody CartItemRequest request) {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.updateCartItem(currentUser, itemId, request); 
        ApiResponse<CartDto> response = ApiResponse.success("Update cart item successfully", cartDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> removeItemFromCart(@PathVariable Long itemId) {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.removeItemFromCart(currentUser, itemId); 
        ApiResponse<CartDto> response = ApiResponse.success("Remove item from cart successfully", cartDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CartDto>> clearCart() {
        User currentUser = userService.getCurrentUser(); 
        CartDto cartDto = cartService.clearCart(currentUser); 
        ApiResponse<CartDto> response = ApiResponse.success("Clear cart successfully", cartDto);
        return ResponseEntity.ok(response);
    }
}