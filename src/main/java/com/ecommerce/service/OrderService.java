package com.ecommerce.service;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.dto.OrderDto;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.model.Cart;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.User;
import com.ecommerce.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderMapper orderMapper;
    
    public Page<OrderDto> getUserOrders(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        Page<Order> orders= orderRepository.findByUserId(currentUser.getId(), pageable);
        return orderMapper.toDtoPage(orders);
    }

    public OrderDto getOrderById(Long id) {
        User currentUser = userService.getCurrentUser();
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        // Check if the order belongs to the current user
        if (!order.getUser().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new RuntimeException("You don't have permission to view this order");
        }
        
        OrderDto orderDto = orderMapper.toDto(order);

        return orderDto;
    }
    
    /**
     * Lấy entity Order theo ID (sử dụng nội bộ)
     */
    public Order getOrderEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }
    
    /**
     * Tìm đơn hàng theo mã đơn hàng
     */
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    public OrderDto createOrder(OrderRequest orderRequest) {
        User currentUser = userService.getCurrentUser();
        Cart cart = cartService.getOrCreateCart();
        
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot create order with empty cart");
        }
        
        // Check stock availability before creating order
        productService.checkStockAvailability(cart.getItems());
        
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
            productService.updateProductStock(savedOrder.getItems());
        }
        
        // Clear the cart after creating the order
        cartService.clearCart();
        
        return orderMapper.toDto(savedOrder);
    }

    @Transactional
    public OrderDto cancelOrder(Long id) {
        User currentUser = userService.getCurrentUser();
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        // Check if the order belongs to the current user
        if (!order.getUser().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new RuntimeException("You don't have permission to cancel this order");
        }
        
        // Check if the order can be cancelled
        if (order.getStatus() != Order.OrderStatus.PENDING && 
                order.getStatus() != Order.OrderStatus.PROCESSING) {
            throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        
        // Restore product stock when order is cancelled
        productService.restoreProductStock(order.getItems());
        
        return orderMapper.toDto(orderRepository.save(order));
    }

    public Page<OrderDto> getAllOrders(Pageable pageable) {
        Page<Order> orders =  orderRepository.findAll(pageable);
        return orderMapper.toDtoPage(orders);
    }

    @Transactional
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
            productService.updateProductStock(order.getItems());
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
    
    // /**
    //  * Cập nhật trạng thái đơn hàng theo mã đơn hàng
    //  */
    // @Transactional
    // public OrderDto updateOrderStatusByOrderNumber(String orderNumber, Order.OrderStatus status) {
    //     Order order = orderRepository.findByOrderNumber(orderNumber)
    //             .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        
    //     // Nếu đơn hàng đang ở trạng thái PENDING và chuyển sang PROCESSING
    //     // và phương thức thanh toán là VNPAY, cập nhật stock
    //     if (order.getStatus() == Order.OrderStatus.PENDING && 
    //         status == Order.OrderStatus.PROCESSING && 
    //         "VNPAY".equals(order.getPaymentMethod())) {
    //         productService.updateProductStock(order.getItems());
    //     }
        
    //     order.setStatus(status);
        
    //     if (status == Order.OrderStatus.DELIVERED) {
    //         order.setDeliveryDate(LocalDateTime.now());
    //     }
        
    //     return orderMapper.toDto(orderRepository.save(order));
    // }
    
    // /**
    //  * Tạo URL thanh toán VNPay cho đơn hàng
    //  */
    // public String createVNPayPaymentUrl(Order order, String ipAddress) {
    //     return vnPayService.createPaymentUrl(order, ipAddress);
    // }

}