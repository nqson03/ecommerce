package com.ecommerce.service.interfaces;

import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.model.Cart;

public interface CartService {
    
    Cart getOrCreateCart(Long userId);
    
    CartDto getCart(Long userId);
    
    CartDto addItemToCart(Long userId, CartItemRequest request);
    
    CartDto updateCartItem(Long userId, Long itemId, CartItemRequest request);
    
    CartDto removeItemFromCart(Long userId, Long itemId);
    
    CartDto clearCart(Long userId);
} 