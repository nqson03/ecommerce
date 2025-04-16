package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.OrderDto;
import com.ecommerce.model.Order;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private OrderService orderService;

    /**
     * Tạo URL thanh toán VNPay cho đơn hàng
     * @param orderId ID của đơn hàng cần thanh toán
     * @param request HttpServletRequest để lấy địa chỉ IP
     * @return URL thanh toán VNPay
     */
    @GetMapping("/create-payment/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<?>> createPayment(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            // Lấy thông tin đơn hàng
            OrderDto orderDto = orderService.getOrderById(orderId);
            Order order = orderService.getOrderEntityById(orderId);
            
            // Kiểm tra nếu đơn hàng đã thanh toán
            if (!order.getPaymentMethod().equals("VNPAY") || 
                order.getStatus() != Order.OrderStatus.PENDING) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Invalid order or payment has already been made"));
            }
            
            // Lấy địa chỉ IP của người dùng
            String ipAddress = request.getRemoteAddr();
            
            // Tạo URL thanh toán
            String paymentUrl = vnPayService.createPaymentUrl(order, ipAddress);
            
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            
            return ResponseEntity.ok(ApiResponse.success("Payment URL created successfully",response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Error creating payment URL: " + e.getMessage()));
        }
    }

    /**
     * Xử lý callback từ VNPay
     * @param request HttpServletRequest chứa các tham số từ VNPay
     * @return Chuyển hướng đến trang frontend thông báo kết quả thanh toán
     */
    @Value("${app.frontend.payment-result-url}")
    private String frontendPaymentResultUrl;

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (paramValue != null && !paramValue.isEmpty()) {
                vnpParams.put(paramName, paramValue);
            }
        }
        
        // Xác thực chữ ký từ VNPay
        if (!vnPayService.validateCallback(vnpParams)) {
            return "redirect:" + frontendPaymentResultUrl + "?status=error&message=Invalid-signature";
        }
        
        // Lấy mã đơn hàng
        String orderNumber = vnpParams.get("vnp_TxnRef");
        
        // Kiểm tra trạng thái thanh toán
        boolean paymentSuccess = vnPayService.isPaymentSuccess(vnpParams);
        
        if (paymentSuccess) {
            // Cập nhật trạng thái đơn hàng
            orderService.updateOrderStatus(null,orderNumber, Order.OrderStatus.PROCESSING);
            return "redirect:" + frontendPaymentResultUrl + "?status=success&orderNumber=" + orderNumber;
        } else {
            return "redirect:" + frontendPaymentResultUrl + "?status=error&orderNumber=" + orderNumber;
        }
    }
}