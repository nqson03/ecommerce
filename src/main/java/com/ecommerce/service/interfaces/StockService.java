package com.ecommerce.service.interfaces;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.OrderItem;

import java.util.List;

public interface StockService {
    
    /**
     * Kiểm tra stock khả dụng (tính cả reservation) cho cart items
     */
    void checkAvailableStockForCart(List<CartItem> cartItems);
    
    void updateProductStock(List<OrderItem> orderItems);
    
    void restoreProductStock(List<OrderItem> orderItems);
} 