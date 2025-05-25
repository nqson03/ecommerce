package com.ecommerce.service.impl;

import com.ecommerce.service.interfaces.StockReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StockReservationCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockReservationCleanupService.class);
    
    @Autowired
    private StockReservationService stockReservationService;
    
    /**
     * Chạy mỗi 5 phút để kiểm tra và xử lý các reservation đã hết hạn
     */
    @Scheduled(fixedRate = 300000) // 5 phút = 300,000 milliseconds
    public void cleanupExpiredReservations() {
        try {
            logger.info("Starting cleanup of expired stock reservations");
            stockReservationService.processExpiredReservations();
            logger.info("Completed cleanup of expired stock reservations");
        } catch (Exception e) {
            logger.error("Error during stock reservation cleanup", e);
        }
    }
} 