package com.ecommerce.service.impl;

import java.util.List;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.interfaces.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import com.ecommerce.config.CacheConfig;
import org.springframework.stereotype.Service;

@Service
public class StockServiceImpl implements StockService {
    
    @Autowired
    private ProductRepository productRepository;

    public void checkStockAvailability(List<CartItem> cartItems) {
        for (var cartItem : cartItems) {
            Product product = cartItem.getProduct();
            Integer requestedQuantity = cartItem.getQuantity();
            
            if (product.getStock() < requestedQuantity) {
                throw new RuntimeException("Insufficient quantity of product " + product.getName() +
                    " in stock. Only " + product.getStock() + " items remaining.");
            }
        }
    }
    
    public void updateProductStock(List<OrderItem> orderItems) {
        for (var orderItem : orderItems) {
            updateSingleProductStock(orderItem);
        }
    }
    
    @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#orderItem.product.id")
    private void updateSingleProductStock(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        Integer orderedQuantity = orderItem.getQuantity();
        
        product.setStock(product.getStock() - orderedQuantity);
        productRepository.save(product);
    }
    
    public void restoreProductStock(List<OrderItem> orderItems) {
        for (var orderItem : orderItems) {
            restoreSingleProductStock(orderItem);
        }
    }
    
    @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#orderItem.product.id")
    private void restoreSingleProductStock(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        Integer orderedQuantity = orderItem.getQuantity();
        
        product.setStock(product.getStock() + orderedQuantity);
        productRepository.save(product);
    }
} 