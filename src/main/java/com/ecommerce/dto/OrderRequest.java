package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderRequest {
    @NotBlank
    private String shippingAddress;
    
    @NotBlank
    private String paymentMethod; // Có thể là: "COD", "VNPAY", "BANK_TRANSFER"
    
    // Thông tin bổ sung cho thanh toán VNPay
    private String clientIp; // Địa chỉ IP của khách hàng
    private String vnpBankCode; // Mã ngân hàng (có thể để trống để hiển thị tất cả)
    private String vnpLocale; // Ngôn ngữ hiển thị (vn/en)
}