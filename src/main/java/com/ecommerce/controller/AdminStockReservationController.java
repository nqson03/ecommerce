package com.ecommerce.controller;

import com.ecommerce.repository.StockReservationRepository;
import com.ecommerce.service.interfaces.StockReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stock-reservations")
@Tag(name = "Admin Stock Reservation Management", description = "APIs for managing stock reservations cleanup")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStockReservationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminStockReservationController.class);

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Value("${app.stock-reservation.cleanup.retention-days:30}")
    private int defaultRetentionDays;

    @Value("${app.stock-reservation.cleanup.batch-size:1000}")
    private int batchSize;

    @Value("${app.stock-reservation.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @PostMapping("/cleanup/old")
    @Operation(summary = "Manually trigger cleanup of old reservations")
    public ResponseEntity<Map<String, Object>> cleanupOldReservations(
            @Parameter(description = "Number of days to retain (default from config)") @RequestParam(required = false) Integer retentionDays) {
        try {
            int days = retentionDays != null ? retentionDays : defaultRetentionDays;
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

            logger.info("Manual cleanup of old reservations triggered by admin (retention: {} days)", days);
            stockReservationService.deleteOldReservations(cutoffDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Old reservations cleanup completed successfully");
            response.put("retentionDays", days);
            response.put("cutoffDate", cutoffDate);
            response.put("batchSize", batchSize);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during manual old reservations cleanup", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error during cleanup: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/cleanup/info")
    @Operation(summary = "Get cleanup configuration and statistics")
    public ResponseEntity<Map<String, Object>> getCleanupInfo(
            @Parameter(description = "Number of days to check for statistics (default from config)") @RequestParam(required = false) Integer retentionDays) {

        int days = retentionDays != null ? retentionDays : defaultRetentionDays;
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        // Get statistics
        Long oldReservationsCount = stockReservationRepository.countOldReservations(cutoffDate);
        Long expiredReservationsCount = (long) stockReservationRepository.findExpiredReservations(LocalDateTime.now())
                .size();

        // Calculate batch estimates
        int estimatedBatches = oldReservationsCount > 0 ? (int) Math.ceil((double) oldReservationsCount / batchSize)
                : 0;
        long estimatedTimeMinutes = estimatedBatches * 2; // Ước tính 2 phút/batch

        Map<String, Object> response = new HashMap<>();

        // Configuration info (từ /cleanup/status)
        response.put("cleanupEnabled", cleanupEnabled);
        response.put("defaultRetentionDays", defaultRetentionDays);
        response.put("batchSize", batchSize);

        // Statistics info (từ /cleanup/stats)
        response.put("retentionDays", days);
        response.put("cutoffDate", cutoffDate);
        response.put("oldReservationsCount", oldReservationsCount);
        response.put("expiredReservationsCount", expiredReservationsCount);
        response.put("estimatedBatches", estimatedBatches);
        response.put("estimatedTimeMinutes", estimatedTimeMinutes);

        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}