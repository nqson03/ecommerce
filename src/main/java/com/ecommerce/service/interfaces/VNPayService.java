package com.ecommerce.service.interfaces;

import com.ecommerce.dto.PaymentResult;
import com.ecommerce.model.Order;

import java.util.Map;

public interface VNPayService {
    
    String createPaymentUrl(Order order, String ipAddress);
    
    boolean validateCallback(Map<String, String> vnpParams);
    
    boolean isPaymentSuccess(Map<String, String> vnpParams);
    
    PaymentResult getPaymentResult(Map<String, String> vnpParams);
} 