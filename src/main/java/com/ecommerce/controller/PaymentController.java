package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.PaymentResult;
import com.ecommerce.service.interfaces.PaymentService;
import com.ecommerce.service.interfaces.PaymentUrlBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentUrlBuilder urlBuilder;

    /**
     * Tạo URL thanh toán VNPay cho đơn hàng
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
            String username = getCurrentUsername();
            String ipAddress = request.getRemoteAddr();
            
            String paymentUrl = paymentService.createPaymentUrl(orderId, username, ipAddress);
            
            Map<String, String> response = Map.of("paymentUrl", paymentUrl);
            return ResponseEntity.ok(ApiResponse.success("Payment URL created successfully", response));
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating payment URL for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error creating payment URL: " + e.getMessage()));
        }
    }

    /**
     * Xử lý callback từ VNPay
     */
    @Operation(summary = "Xử lý callback từ VNPay", description = "Xử lý kết quả thanh toán từ VNPay và chuyển hướng đến trang frontend")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "Chuyển hướng đến trang kết quả thanh toán")
    })
    @GetMapping("/vnpay-return")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60, anonymousLimit = 10)
    public String vnpayReturn(@Parameter(description = "Request chứa thông tin từ VNPay") HttpServletRequest request) {
        
        logger.info("Received VNPay return callback: {}", request.getQueryString());
        
        try {
            Map<String, String> vnpParams = extractVnpParams(request);
            PaymentResult result = paymentService.processReturnCallback(vnpParams);
            String orderNumber = vnpParams.get("vnp_TxnRef");

            if (result.isSuccess()) {
                return "redirect:" + urlBuilder.buildSuccessUrl(orderNumber, result);
            } else {
                return "redirect:" + urlBuilder.buildErrorUrl(orderNumber, result);
            }
            
        } catch (IllegalArgumentException e) {
            // Invalid signature
            return "redirect:" + urlBuilder.buildInvalidSignatureUrl();
        } catch (Exception e) {
            logger.error("Error processing payment callback: {}", e.getMessage(), e);
            return "redirect:" + urlBuilder.buildErrorUrl(null, "Lỗi xử lý thanh toán");
        }
    }

    /**
     * Xử lý IPN (Instant Payment Notification) từ VNPay
     */
    @Operation(summary = "Xử lý IPN từ VNPay", description = "Xử lý thông báo thanh toán tức thì từ VNPay")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xử lý IPN thành công")
    })
    @PostMapping("/vnpay-ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> vnpayIPN(HttpServletRequest request) {
        logger.info("Received VNPay IPN: {}", request.getQueryString());
        
        try {
            Map<String, String> vnpParams = extractVnpParams(request);
            PaymentService.IPNResponse ipnResponse = paymentService.processIPNCallback(vnpParams);
            
            Map<String, String> response = Map.of(
                "RspCode", ipnResponse.getRspCode(),
                "Message", ipnResponse.getMessage()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing VNPay IPN: {}", e.getMessage(), e);
            Map<String, String> errorResponse = Map.of(
                "RspCode", "99",
                "Message", "Unknown error"
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Utility method để lấy username hiện tại
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    /**
     * Utility method để extract VNPay parameters từ request
     */
    private Map<String, String> extractVnpParams(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (paramValue != null && !paramValue.isEmpty()) {
                vnpParams.put(paramName, paramValue);
            }
        }
        
        return vnpParams;
    }
}