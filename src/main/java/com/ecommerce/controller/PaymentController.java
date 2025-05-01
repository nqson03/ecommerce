package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.model.Order;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
// Using fully qualified name for Swagger ApiResponse to avoid collision with DTO ApiResponse
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Controller;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "API quản lý thanh toán")
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
    @Operation(summary = "Tạo URL thanh toán", description = "Tạo URL thanh toán VNPay cho đơn hàng", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo URL thanh toán thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Đơn hàng không hợp lệ hoặc đã thanh toán",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng",
                content = @Content)
    })
    @GetMapping("/create-payment/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> createPayment(
            @Parameter(description = "ID của đơn hàng", required = true) @PathVariable Long orderId, 
            HttpServletRequest request) {
        try {
           // Lấy thông tin người dùng hiện tại
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
        
            // Lấy thông tin đơn hàng
            Order order = orderService.getOrderEntityById(orderId);
        
            // Kiểm tra xem người dùng hiện tại có phải là chủ sở hữu của đơn hàng không
            if (!order.getUser().getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("You are not authorized to make payment for this order"));
            }
            
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

    @Operation(summary = "Xử lý callback từ VNPay", description = "Xử lý kết quả thanh toán từ VNPay và chuyển hướng đến trang frontend")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "Chuyển hướng đến trang kết quả thanh toán")
    })
    @GetMapping("/vnpay-return")
    // @RateLimit(authenticatedLimit = 20, refreshPeriod = 60, anonymousLimit = 10)
    public String vnpayReturn(
            @Parameter(description = "Request chứa thông tin từ VNPay") HttpServletRequest request) {
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