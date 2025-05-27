package com.ecommerce.service.interfaces;

import com.ecommerce.dto.ProductResponse;
import com.ecommerce.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Product Cache operations
 * Handles caching of static product data (without real-time stock info)
 */
public interface ProductCacheService {
    
    /**
     * Get cached static product data by ID
     * @param id Product ID
     * @return ProductResponse with static data only (no availableStock/reservedStock)
     */
    ProductResponse getStaticProduct(Long id);
    
    /**
     * Get cached static products with pagination
     * @param pageable Pagination info
     * @return Page of ProductResponse with static data only
     */
    Page<ProductResponse> getStaticProducts(Pageable pageable);
    
    /**
     * Get cached static products by category with pagination
     * @param categoryId Category ID
     * @param pageable Pagination info
     * @return Page of ProductResponse with static data only
     */
    Page<ProductResponse> getStaticProductsByCategory(Long categoryId, Pageable pageable);
    
    /**
     * Get cached static products by search keyword with pagination
     * @param keyword Search keyword
     * @param pageable Pagination info
     * @return Page of ProductResponse with static data only
     */
    Page<ProductResponse> getStaticSearchProducts(String keyword, Pageable pageable);
    
    /**
     * Evict product cache by ID
     * @param id Product ID
     */
    void evictProductCache(Long id);
    
    /**
     * Evict all product-related caches
     */
    void evictAllProductCaches();

    /**
     * Updates the cached ProductResponse for a given Product entity.
     * This method is typically annotated with @CachePut.
     * @param product The Product entity with updated information.
     * @return The ProductResponse that was put into the cache.
     */
    ProductResponse updateCachedProduct(Product product);
} 