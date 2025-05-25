package com.ecommerce.service.interfaces;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.StockReservation;

import java.util.List;

public interface StockReservationService {
    
    /**
     * Tạo reservation cho các items trong order (dành cho VNPAY)
     */
    List<StockReservation> createReservations(Order order);
    
    /**
     * Xác nhận reservation khi thanh toán thành công
     */
    void confirmReservations(Long orderId);
    
    /**
     * Hủy reservation khi thanh toán thất bại hoặc order bị hủy
     */
    void cancelReservations(Long orderId);
    
    /**
     * Kiểm tra và hủy các reservation đã hết hạn
     */
    void processExpiredReservations();
    
    /**
     * Kiểm tra stock khả dụng (stock thực tế - stock đã reserved)
     */
    Integer getAvailableStock(Long productId);
    
    /**
     * Kiểm tra xem có đủ stock khả dụng cho order không
     */
    boolean checkAvailableStockForOrder(List<OrderItem> orderItems);
} 