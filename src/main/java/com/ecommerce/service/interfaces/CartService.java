package com.ecommerce.service.interfaces;

import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.model.Cart;
import com.ecommerce.model.User;

public interface CartService {
    
    Cart getOrCreateCart(User currentUser);
    
    CartDto getCart(User currentUser);
    
    CartDto addItemToCart(User currentUser, CartItemRequest request);
    
    CartDto updateCartItem(User currentUser, Long itemId, CartItemRequest request);
    
    CartDto removeItemFromCart(User currentUser, Long itemId);
    
    CartDto clearCart(User currentUser);
} 