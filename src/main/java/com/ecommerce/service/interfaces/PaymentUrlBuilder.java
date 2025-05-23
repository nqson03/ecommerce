package com.ecommerce.service.interfaces;

import com.ecommerce.dto.PaymentResult;

public interface PaymentUrlBuilder {
    
    String buildSuccessUrl(String orderNumber, PaymentResult result);
    
    String buildErrorUrl(String orderNumber, PaymentResult result);
    
    String buildErrorUrl(String orderNumber, String message);
    
    String buildInvalidSignatureUrl();
} 