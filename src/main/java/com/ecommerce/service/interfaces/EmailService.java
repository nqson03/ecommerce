package com.ecommerce.service.interfaces;

import com.ecommerce.model.Order;
import com.ecommerce.model.User;

public interface EmailService {
    
    /**
     * Gửi email xác nhận đặt hàng thành công
     */
    void sendOrderConfirmationEmail(User user, Order order);
    
    /**
     * Gửi email thông báo đặt hàng thất bại
     */
    void sendOrderFailureEmail(User user, Order order, String reason);
    
    /**
     * Gửi email cập nhật trạng thái đơn hàng
     */
    void sendOrderStatusUpdateEmail(User user, Order order);
    
    /**
     * Gửi email hủy đơn hàng
     */
    void sendOrderCancellationEmail(User user, Order order);
} 