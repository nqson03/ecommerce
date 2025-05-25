package com.ecommerce.repository;

import com.ecommerce.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    
    List<StockReservation> findByOrderId(Long orderId);
    
    List<StockReservation> findByProductIdAndStatus(Long productId, StockReservation.ReservationStatus status);
    
    @Query("SELECT SUM(sr.quantity) FROM StockReservation sr WHERE sr.product.id = :productId AND sr.status = 'ACTIVE'")
    Integer getTotalReservedQuantityForProduct(@Param("productId") Long productId);
    
    @Query("SELECT sr FROM StockReservation sr WHERE sr.status = 'ACTIVE' AND sr.expiresAt < :currentTime")
    List<StockReservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT sr FROM StockReservation sr WHERE sr.order.id = :orderId AND sr.status = 'ACTIVE'")
    List<StockReservation> findActiveReservationsByOrderId(@Param("orderId") Long orderId);
    
    @Modifying
    @Query("DELETE FROM StockReservation sr WHERE sr.createdAt < :cutoffDate AND sr.status IN ('CONFIRMED', 'CANCELLED', 'EXPIRED')")
    int deleteOldReservations(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Modifying
    @Query(value = "DELETE FROM stock_reservations WHERE created_at < :cutoffDate AND status IN ('CONFIRMED', 'CANCELLED', 'EXPIRED') LIMIT :batchSize", nativeQuery = true)
    int deleteOldReservationsBatch(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("batchSize") int batchSize);
    
    @Query("SELECT COUNT(sr) FROM StockReservation sr WHERE sr.createdAt < :cutoffDate AND sr.status IN ('CONFIRMED', 'CANCELLED', 'EXPIRED')")
    Long countOldReservations(@Param("cutoffDate") LocalDateTime cutoffDate);
} 