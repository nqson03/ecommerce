package com.ecommerce.service.impl;

import com.ecommerce.config.CacheConfig;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.dto.CartDto;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.interfaces.CartService;
import com.ecommerce.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CartMapper cartMapper;
    
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        User user = userService.getUserById(userId);
                
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    @Cacheable(value = CacheConfig.CART_CACHE, key = "#userId") 
    public CartDto getCart(Long userId) { 
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toDto(cart);
    }

    @Transactional
    @CachePut(value = CacheConfig.CART_CACHE, key = "#userId") 
    public CartDto addItemToCart(Long userId, CartItemRequest request) { 
        Cart cart = getOrCreateCart(userId);
        
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        
        // Check if the product is already in the cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();
        
        if (existingItem.isPresent()) {
            // Update quantity if product already exists in cart
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            // Add new item to cart
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }
        
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    @Transactional
    @CachePut(value = CacheConfig.CART_CACHE, key = "#userId") 
    public CartDto updateCartItem(Long userId, Long itemId, CartItemRequest request) { 
        Cart cart = getOrCreateCart(userId);
        
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId + " in cart for user: " + userId));
        
        // Optional: Check if the product ID in the request matches the item's product ID
        // This prevents accidentally changing the product associated with a cart item ID
        if (!item.getProduct().getId().equals(request.getProductId())) {
             Product product = productRepository.findById(request.getProductId())
                 .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
             item.setProduct(product);
        }

        item.setQuantity(request.getQuantity());
        
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    @Transactional
    @CachePut(value = CacheConfig.CART_CACHE, key = "#userId") 
    public CartDto removeItemFromCart(Long userId, Long itemId) { 
        Cart cart = getOrCreateCart(userId);

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
    
        // Nếu không có mục nào bị xóa, tức là itemId không tồn tại trong giỏ, thì ném exception
        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found with id: " + itemId + " in cart for user: " + userId);
        }
        
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    @Transactional
    @CachePut(value = CacheConfig.CART_CACHE, key = "#userId") 
    public CartDto clearCart(Long userId) { 
        Cart cart = getOrCreateCart(userId);
        
        // Instead of clearing, remove items associated with the cart to ensure cascading deletes if configured
        cart.getItems().clear(); // Or iterate and remove if clear() doesn't trigger cascades properly
        
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }
} 