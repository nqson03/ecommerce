package com.ecommerce.service.impl;

import java.util.List;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.interfaces.StockService;
import com.ecommerce.service.interfaces.StockReservationService;
import com.ecommerce.service.interfaces.ProductCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockServiceImpl implements StockService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private StockReservationService stockReservationService;
    
    @Autowired
    private ProductCacheService productCacheService;
    
    @Override
    public void checkAvailableStockForCart(List<CartItem> cartItems) {
        for (var cartItem : cartItems) {
            Product product = cartItem.getProduct();
            Integer requestedQuantity = cartItem.getQuantity();
            Integer availableStock = stockReservationService.getAvailableStock(product.getId());
            
            if (availableStock < requestedQuantity) {
                throw new InsufficientStockException("Insufficient available quantity of product " + product.getName() +
                    ". Only " + availableStock + " items available (considering reservations).");
            }
        }
    }
    
    public void updateProductStock(List<OrderItem> orderItems) {
        for (var orderItem : orderItems) {
            Product product = orderItem.getProduct();
            Integer orderedQuantity = orderItem.getQuantity();
            
            product.setStock(product.getStock() - orderedQuantity);
            productRepository.save(product);
            
            // Evict cache using external service call
            productCacheService.evictProductCache(product.getId());
        }
        // Evict all product lists vì actual stock đã thay đổi
        productCacheService.evictAllProductCaches();
    }
    
    public void restoreProductStock(List<OrderItem> orderItems) {
        for (var orderItem : orderItems) {
            Product product = orderItem.getProduct();
            Integer orderedQuantity = orderItem.getQuantity();
            
            product.setStock(product.getStock() + orderedQuantity);
            productRepository.save(product);
            
            // Evict cache using external service call
            productCacheService.evictProductCache(product.getId());
        }
        // Evict all product lists vì actual stock đã thay đổi
        productCacheService.evictAllProductCaches();
    }
}