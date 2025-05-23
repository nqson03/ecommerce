package com.ecommerce.service.interfaces;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.OrderItem;

import java.util.List;

public interface StockService {
    
    void checkStockAvailability(List<CartItem> cartItems);
    
    void updateProductStock(List<OrderItem> orderItems);
    
    void restoreProductStock(List<OrderItem> orderItems);
} 