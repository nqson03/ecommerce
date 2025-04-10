package com.ecommerce.mapper;

import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemDto;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    @Autowired
    private EntityMapper entityMapper;
    
    /**
     * Chuyển đổi Cart thành CartDto
     */
    public CartDto toDto(Cart cart) {
        CartDto dto = entityMapper.convertToDto(cart, CartDto.class);
        
        // Xử lý user
        if (cart.getUser() != null) {
            dto.setUserId(cart.getUser().getId());
            dto.setUsername(cart.getUser().getUsername());
        }
        
        // Xử lý items
        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            dto.setItems(cart.getItems().stream()
                    .map(this::convertCartItemToDto)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    /**
     * Chuyển đổi CartItem thành CartItemDto
     */
    private CartItemDto convertCartItemToDto(CartItem cartItem) {
        CartItemDto dto = entityMapper.convertToDto(cartItem, CartItemDto.class);
        
        if (cartItem.getCart() != null) {
            dto.setCartId(cartItem.getCart().getId());
        }
        
        if (cartItem.getProduct() != null) {
            dto.setProductId(cartItem.getProduct().getId());
            dto.setProductName(cartItem.getProduct().getName());
            if (cartItem.getProduct().getImages() != null && !cartItem.getProduct().getImages().isEmpty()) {
                dto.setProductImage(cartItem.getProduct().getImages().get(0));
            }
        }
        
        // Tính tổng giá
        if (cartItem.getPrice() != null && cartItem.getQuantity() != null) {
            dto.setTotalPrice(cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }
        
        return dto;
    }
}