package com.ecommerce.service.interfaces;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderService {
    
    Page<OrderDto> getUserOrders(Long userId, Pageable pageable);
    
    OrderDto getOrderById(Long id, Long userId);
    
    Order getOrderEntityById(Long id);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    OrderDto createOrder(OrderRequest orderRequest, Long userId);
    
    OrderDto cancelOrder(Long id, Long userId);
    
    Page<OrderDto> getAllOrders(Pageable pageable);
    
    OrderDto updateOrderStatus(Long id, String orderNumber, Order.OrderStatus status);

} 