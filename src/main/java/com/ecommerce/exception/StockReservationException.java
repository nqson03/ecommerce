package com.ecommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StockReservationException extends RuntimeException {
    public StockReservationException(String message) {
        super(message);
    }
    
    public StockReservationException(String message, Throwable cause) {
        super(message, cause);
    }
} 