package com.ecommerce.service.impl;

import com.ecommerce.dto.PaymentResult;
import com.ecommerce.service.interfaces.PaymentUrlBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class PaymentUrlBuilderImpl implements PaymentUrlBuilder {

    @Value("${app.frontend.payment-result-url}")
    private String frontendPaymentResultUrl;

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

    public String buildErrorUrl(String orderNumber, PaymentResult result) {
        StringBuilder url = new StringBuilder(frontendPaymentResultUrl)
            .append("?status=error")
            .append("&orderNumber=").append(orderNumber)
            .append("&message=").append(URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8))
            .append("&code=").append(result.getResponseCode());
        
        return url.toString();
    }

    public String buildErrorUrl(String orderNumber, String message) {
        return new StringBuilder(frontendPaymentResultUrl)
            .append("?status=error")
            .append("&orderNumber=").append(orderNumber != null ? orderNumber : "")
            .append("&message=").append(URLEncoder.encode(message, StandardCharsets.UTF_8))
            .toString();
    }

    public String buildInvalidSignatureUrl() {
        return frontendPaymentResultUrl + "?status=error&message=Invalid-signature";
    }
} 