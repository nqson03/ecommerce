package com.ecommerce.service.impl;

import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.StockReservation;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.StockReservationRepository;
import com.ecommerce.service.interfaces.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockReservationServiceImpl implements StockReservationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockReservationServiceImpl.class);
    
    @Autowired
    private StockReservationRepository stockReservationRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Value("${app.stock-reservation.cleanup.batch-size:1000}")
    private int batchSize;
    
    @Override
    @Transactional
    public List<StockReservation> createReservations(Order order) {
        List<StockReservation> reservations = new ArrayList<>();
        
        // Kiểm tra stock khả dụng trước khi tạo reservation
        if (!checkAvailableStockForOrder(order.getItems())) {
            throw new InsufficientStockException("Insufficient stock available for reservation");
        }
        
        for (OrderItem orderItem : order.getItems()) {
            StockReservation reservation = new StockReservation();
            reservation.setProduct(orderItem.getProduct());
            reservation.setOrder(order);
            reservation.setQuantity(orderItem.getQuantity());
            reservation.setStatus(StockReservation.ReservationStatus.ACTIVE);
            reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30)); // 30 phút timeout
            
            reservations.add(stockReservationRepository.save(reservation));
        }
        
        return reservations;
    }
    
    @Override
    @Transactional
    public void confirmReservations(Long orderId) {
        List<StockReservation> reservations = stockReservationRepository.findActiveReservationsByOrderId(orderId);
        
        for (StockReservation reservation : reservations) {
            // Cập nhật trạng thái reservation
            reservation.setStatus(StockReservation.ReservationStatus.CONFIRMED);
            reservation.setConfirmedAt(LocalDateTime.now());
            stockReservationRepository.save(reservation);
            
            // Cập nhật stock thực tế của product
            Product product = reservation.getProduct();
            product.setStock(product.getStock() - reservation.getQuantity());
            productRepository.save(product);
        }
    }
    
    @Override
    @Transactional
    public void cancelReservations(Long orderId) {
        List<StockReservation> reservations = stockReservationRepository.findActiveReservationsByOrderId(orderId);
        
        for (StockReservation reservation : reservations) {
            reservation.setStatus(StockReservation.ReservationStatus.CANCELLED);
            reservation.setCancelledAt(LocalDateTime.now());
            stockReservationRepository.save(reservation);
        }
    }
    
    @Override
    @Transactional
    public void processExpiredReservations() {
        List<StockReservation> expiredReservations = 
            stockReservationRepository.findExpiredReservations(LocalDateTime.now());
        
        for (StockReservation reservation : expiredReservations) {
            reservation.setStatus(StockReservation.ReservationStatus.EXPIRED);
            reservation.setCancelledAt(LocalDateTime.now());
            stockReservationRepository.save(reservation);
        }
    }
    
    @Override
    public Integer getAvailableStock(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Integer reservedQuantity = stockReservationRepository.getTotalReservedQuantityForProduct(productId);
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
        
        return product.getStock() - reservedQuantity;
    }
    
    @Override
    public Integer getTotalReservedQuantityForProduct(Long productId) {
        Integer reservedQuantity = stockReservationRepository.getTotalReservedQuantityForProduct(productId);
        return reservedQuantity != null ? reservedQuantity : 0;
    }
    
    @Override
    public boolean checkAvailableStockForOrder(List<OrderItem> orderItems) {
        for (OrderItem orderItem : orderItems) {
            Integer availableStock = getAvailableStock(orderItem.getProduct().getId());
            if (availableStock < orderItem.getQuantity()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    @Transactional
    public void deleteOldReservations(LocalDateTime cutoffDate) {
        Long totalCount = stockReservationRepository.countOldReservations(cutoffDate);
        logger.info("Found {} old stock reservations to delete before {} (batch size: {})", 
                   totalCount, cutoffDate, batchSize);
        
        if (totalCount == 0) {
            logger.info("No old stock reservations found to delete");
            return;
        }
        
        int totalDeleted = 0;
        int batchCount = 0;
        long startTime = System.currentTimeMillis();
        
        // Xử lý theo batch để tránh lock database quá lâu
        while (totalDeleted < totalCount) {
            batchCount++;
            logger.debug("Processing batch {} (size: {})", batchCount, batchSize);
            
            try {
                int deletedInBatch = stockReservationRepository.deleteOldReservationsBatch(cutoffDate, batchSize);
                totalDeleted += deletedInBatch;
                
                logger.info("Batch {}: Deleted {} records. Total deleted: {}/{}", 
                           batchCount, deletedInBatch, totalDeleted, totalCount);
                
                if (deletedInBatch == 0) {
                    logger.info("No more records to delete, stopping batch processing");
                    break;
                }
                
                // Nghỉ ngắn giữa các batch để giảm load database
                if (deletedInBatch == batchSize && totalDeleted < totalCount) {
                    try {
                        Thread.sleep(100); // 100ms pause between batches
                    } catch (InterruptedException e) {
                        logger.warn("Batch processing interrupted");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error in batch {} during deletion", batchCount, e);
                throw e; // Re-throw để rollback transaction
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("Completed deletion: {}/{} records deleted in {} batches (took {}ms)", 
                   totalDeleted, totalCount, batchCount, duration);
    }
} 