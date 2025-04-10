package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.service.CartService;
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

    @GetMapping
    public ResponseEntity<ApiResponse<CartDto>> getCart() {
        CartDto cartDto = cartService.getCart();
        ApiResponse<CartDto> response = ApiResponse.success("Get cart successfully",cartDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDto>> addItemToCart(@Valid @RequestBody CartItemRequest request) {
        CartDto cartDto = cartService.addItemToCart(request);
        ApiResponse<CartDto> response = ApiResponse.success("Add item to cart successfully",cartDto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDto>> updateCartItem(
            @PathVariable Long itemId, @Valid @RequestBody CartItemRequest request) {
        CartDto cartDto = cartService.updateCartItem(itemId, request);
        ApiResponse<CartDto> response = ApiResponse.success("Update cart item successfully",cartDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDto>> removeItemFromCart(@PathVariable Long itemId) {
        CartDto cartDto = cartService.removeItemFromCart(itemId);
        ApiResponse<CartDto> response = ApiResponse.success("Remove item from cart successfully",cartDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartDto>> clearCart() {
        CartDto cartDto = cartService.clearCart();
        ApiResponse<CartDto> response = ApiResponse.success("Clear cart successfully",cartDto);
        return ResponseEntity.ok(response);
    }
}