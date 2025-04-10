package com.ecommerce.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CartMapper cartMapper;

    @Transactional
    public Cart getOrCreateCart() {
        User currentUser = userService.getCurrentUser();
        
        return cartRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(currentUser);
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public CartDto getCart() {
        Cart cart = getOrCreateCart();
        return cartMapper.toDto(cart);
    }

    @Transactional
    public CartDto addItemToCart(CartItemRequest request) {
        Cart cart = getOrCreateCart();
        
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
            item.setPrice(product.getPrice());
        } else {
            // Add new item to cart
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            newItem.setPrice(product.getPrice());
            cart.getItems().add(newItem);
        }
        
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    @Transactional
    public CartDto updateCartItem(Long itemId, CartItemRequest request) {
        Cart cart = getOrCreateCart();
        
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));
        
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        
        item.setProduct(product);
        item.setQuantity(request.getQuantity());
        item.setPrice(product.getPrice());
        
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    @Transactional
    public CartDto removeItemFromCart(Long itemId) {
        Cart cart = getOrCreateCart();
        
        // Cố gắng xóa item có id trùng với itemId
        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
    
        // Nếu không có mục nào bị xóa, tức là itemId không tồn tại trong giỏ, thì ném exception
        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found with id: " + itemId);
        }
        
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    @Transactional
    public CartDto clearCart() {
        Cart cart = getOrCreateCart();
        
        cart.getItems().clear();
        
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }
}