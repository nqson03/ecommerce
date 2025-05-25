package com.ecommerce.dto;

import com.ecommerce.model.StockReservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationDto {
    
    private Long id;
    private Long productId;
    private String productName;
    private Long orderId;
    private String orderNumber;
    private Integer quantity;
    private StockReservation.ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
}