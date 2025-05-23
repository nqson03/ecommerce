package com.ecommerce.service;

import com.ecommerce.dto.PaymentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class PaymentUrlBuilder {

    @Value("${app.frontend.payment-result-url}")
    private String frontendPaymentResultUrl;

    /**
     * Build URL cho thanh toán thành công
     */
    public String buildSuccessUrl(String orderNumber, PaymentResult result) {
        StringBuilder url = new StringBuilder(frontendPaymentResultUrl)
            .append("?status=success")
            .append("&orderNumber=").append(orderNumber)
            .append("&message=").append(URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8));
        
        if (result.getTransactionId() != null && !result.getTransactionId().isEmpty()) {
            url.append("&transactionId=").append(result.getTransactionId());
        }
        
        return url.toString();
    }

    /**
     * Build URL cho thanh toán thất bại với PaymentResult
     */
    public String buildErrorUrl(String orderNumber, PaymentResult result) {
        StringBuilder url = new StringBuilder(frontendPaymentResultUrl)
            .append("?status=error")
            .append("&orderNumber=").append(orderNumber)
            .append("&message=").append(URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8))
            .append("&code=").append(result.getResponseCode());
        
        return url.toString();
    }

    /**
     * Build URL cho lỗi general với message tùy chỉnh
     */
    public String buildErrorUrl(String orderNumber, String message) {
        return new StringBuilder(frontendPaymentResultUrl)
            .append("?status=error")
            .append("&orderNumber=").append(orderNumber != null ? orderNumber : "")
            .append("&message=").append(URLEncoder.encode(message, StandardCharsets.UTF_8))
            .toString();
    }

    /**
     * Build URL cho lỗi chữ ký không hợp lệ
     */
    public String buildInvalidSignatureUrl() {
        return frontendPaymentResultUrl + "?status=error&message=Invalid-signature";
    }
} 