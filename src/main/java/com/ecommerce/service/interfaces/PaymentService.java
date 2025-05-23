package com.ecommerce.service.interfaces;

import com.ecommerce.dto.PaymentResult;

import java.util.Map;

public interface PaymentService {
    
    String createPaymentUrl(Long orderId, String username, String ipAddress);
    
    PaymentResult processReturnCallback(Map<String, String> vnpParams);
    
    IPNResponse processIPNCallback(Map<String, String> vnpParams);
    
    public static class IPNResponse {
        private final String rspCode;
        private final String message;

        public IPNResponse(String rspCode, String message) {
            this.rspCode = rspCode;
            this.message = message;
        }

        public String getRspCode() { 
            return rspCode; 
        }
        
        public String getMessage() { 
            return message; 
        }
    }
} 