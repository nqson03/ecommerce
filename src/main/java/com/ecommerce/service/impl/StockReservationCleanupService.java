package com.ecommerce.service.impl;

import com.ecommerce.service.interfaces.StockReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Service
public class StockReservationCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockReservationCleanupService.class);
    
    @Autowired
    private StockReservationService stockReservationService;
    
    @Value("${app.stock-reservation.cleanup.retention-days:30}")
    private int retentionDays;
    
    @Value("${app.stock-reservation.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
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
            logger.error("Error during expired stock reservation cleanup", e);
        }
    }
    
    /**
     * Chạy hàng ngày lúc 2:00 AM để xóa các reservation cũ đã hoàn thành
     */
    @Scheduled(cron = "0 0 2 * * *") // Chạy lúc 2:00 AM mỗi ngày
    public void cleanupOldReservations() {
        if (!cleanupEnabled) {
            logger.debug("Old reservation cleanup is disabled");
            return;
        }
        
        try {
            logger.info("Starting cleanup of old stock reservations (retention: {} days)", retentionDays);
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            stockReservationService.deleteOldReservations(cutoffDate);
            
            logger.info("Completed cleanup of old stock reservations");
        } catch (Exception e) {
            logger.error("Error during old stock reservation cleanup", e);
        }
    }
} 