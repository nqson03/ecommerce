package com.ecommerce.service.impl;

import com.ecommerce.config.CacheConfig;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.exception.EmptyCartException;
import com.ecommerce.exception.InvalidOrderStatusException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedOperationException;
import com.ecommerce.dto.OrderDto;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.model.Cart;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.interfaces.CartService;
import com.ecommerce.service.interfaces.OrderService;
import com.ecommerce.service.interfaces.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private StockService stockService;

    @Autowired
    private OrderMapper orderMapper;
    
    @Cacheable(value = CacheConfig.USER_ORDERS_CACHE, key = "#userId + '_' + #pageable.toString()")
    public Page<OrderDto> getUserOrders(Long userId, Pageable pageable) {
        Page<Order> orders= orderRepository.findByUserId(userId, pageable);
        return orderMapper.toDtoPage(orders);
    }

    @Cacheable(value = CacheConfig.ORDER_CACHE, key = "#id")
    public OrderDto getOrderById(Long id, User currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        // Check if the order belongs to the current user or if user is admin
        if (!order.getUser().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new UnauthorizedOperationException("You don't have permission to view this order");
        }
        
        OrderDto orderDto = orderMapper.toDto(order);
        return orderDto;
    }
    
    public Order getOrderEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }
    
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_ORDERS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.ORDERS_CACHE, allEntries = true)
    })
    public OrderDto createOrder(OrderRequest orderRequest, User currentUser) {
        Cart cart = cartService.getOrCreateCart(currentUser); 
        
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot create order with empty cart");
        }
        
        // Check stock availability before creating order
        stockService.checkStockAvailability(cart.getItems());
        
        Order order = new Order();
        order.setUser(currentUser);
        order.setOrderNumber(generateOrderNumber());
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        
        // Nếu phương thức thanh toán là VNPay, đặt trạng thái là PENDING
        // và không cập nhật stock cho đến khi thanh toán thành công
        if ("VNPAY".equals(orderRequest.getPaymentMethod())) {
            order.setStatus(Order.OrderStatus.PENDING);
        } else {
            // Với các phương thức thanh toán khác (như COD), đặt trạng thái là PROCESSING
            order.setStatus(Order.OrderStatus.PROCESSING);
        }
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (var cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            
            order.getItems().add(orderItem);
            
            // Calculate total amount
            totalAmount = totalAmount.add(cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }
        
        order.setTotalAmount(totalAmount);
        
        // Save the order first to generate ID
        Order savedOrder = orderRepository.save(order);
        
        // Chỉ cập nhật stock nếu không phải thanh toán VNPay
        if (!"VNPAY".equals(orderRequest.getPaymentMethod())) {
            stockService.updateProductStock(savedOrder.getItems());
        }
        
        // Clear the cart after creating the order
        cartService.clearCart(currentUser); 
        
        return orderMapper.toDto(savedOrder);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.ORDER_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.USER_ORDERS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.ORDERS_CACHE, allEntries = true)
    })
    public OrderDto cancelOrder(Long id, User currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        // Check if the order belongs to the current user or if user is admin
        if (!order.getUser().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new UnauthorizedOperationException("You don't have permission to cancel this order");
        }
        
        // Check if the order can be cancelled
        if (order.getStatus() != Order.OrderStatus.PENDING && 
                order.getStatus() != Order.OrderStatus.PROCESSING) {
            throw new InvalidOrderStatusException("Cannot cancel order with status: " + order.getStatus());
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        
        // Restore product stock when order is cancelled
        stockService.restoreProductStock(order.getItems());
        
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Cacheable(value = CacheConfig.ORDERS_CACHE, key = "#pageable.toString()")
    public Page<OrderDto> getAllOrders(Pageable pageable) {
        Page<Order> orders =  orderRepository.findAll(pageable);
        return orderMapper.toDtoPage(orders);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.ORDER_CACHE, key = "#id", condition = "#id != null"),
        @CacheEvict(value = CacheConfig.USER_ORDERS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.ORDERS_CACHE, allEntries = true)
    })
    public OrderDto updateOrderStatus(Long id, String orderNumber, Order.OrderStatus status) { 
        Order order;
    
        // Tìm đơn hàng theo id hoặc orderNumber
        if (id != null) {
            order = orderRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        } else if (orderNumber != null && !orderNumber.isEmpty()) {
            order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        } else {
            throw new IllegalArgumentException("Either order id or order number must be provided");
        }

        // Nếu đơn hàng đang ở trạng thái PENDING và chuyển sang PROCESSING
        // và phương thức thanh toán là VNPAY, cập nhật stock
        if (order.getStatus() == Order.OrderStatus.PENDING && 
            status == Order.OrderStatus.PROCESSING && 
            "VNPAY".equals(order.getPaymentMethod())) {
            stockService.updateProductStock(order.getItems());
        }

        order.setStatus(status);
        
        if (status == Order.OrderStatus.DELIVERED) {
            order.setDeliveryDate(LocalDateTime.now());
        }
        
        return orderMapper.toDto(orderRepository.save(order));
    }

    private String generateOrderNumber() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
} 