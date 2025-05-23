package com.ecommerce.dto;

import lombok.Data;

@Data
public class PaymentResult {
    private boolean success;
    private String responseCode;
    private String message;
    private String transactionId;
    private String orderNumber;
    private String bankCode;
    private String amount;
    private String payDate;
    
    public PaymentResult() {}
    
    public PaymentResult(boolean success, String responseCode, String message) {
        this.success = success;
        this.responseCode = responseCode;
        this.message = message;
    }
} 