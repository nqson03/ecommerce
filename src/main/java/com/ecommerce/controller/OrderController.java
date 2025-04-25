package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.model.Order;
import com.ecommerce.dto.OrderDto;
import com.ecommerce.model.User; 
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService; 
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService; 

    @GetMapping
    @PreAuthorize("hasRole('USER')") 
    @RateLimit(authenticatedLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getUserOrders(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orderService.getUserOrders(currentUser.getId(), pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") 
    @RateLimit(authenticatedLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Get Order successfully", orderService.getOrderById(id, currentUser)));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')") 
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", orderService.createOrder(orderRequest, currentUser)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") 
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Order canceled successfully", orderService.cancelOrder(id, currentUser)));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully",orderService.getAllOrders(pageable)));
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long id, @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully",orderService.updateOrderStatus(id, null, status)));
    }
}