package com.ecommerce.service.impl;

import com.ecommerce.config.CacheConfig;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.interfaces.ProductCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Implementation of ProductCacheService
 * Handles all caching operations for static product data
 */
@Service
public class ProductCacheServiceImpl implements ProductCacheService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Cacheable(value = CacheConfig.PRODUCT_CACHE, key = "#id")
    public ProductResponse getStaticProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toDtoForCache(product);
    }

    @Override
    @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "#pageable.toString()")
    public Page<ProductResponse> getStaticProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        return productMapper.toDtoPageForCache(products);
    }

    @Override
    @Cacheable(value = CacheConfig.CATEGORY_PRODUCTS_CACHE, key = "#categoryId + '_' + #pageable.toString()")
    public Page<ProductResponse> getStaticProductsByCategory(Long categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);
        return productMapper.toDtoPageForCache(products);
    }

    @Override
    @Cacheable(value = CacheConfig.SEARCH_PRODUCTS_CACHE, key = "#keyword + '_' + #pageable.toString()")
    public Page<ProductResponse> getStaticSearchProducts(String keyword, Pageable pageable) {
        Page<Product> products = productRepository.search(keyword, pageable);
        return productMapper.toDtoPageForCache(products);
    }

    @Override
    @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#id")
    public void evictProductCache(Long id) {
        // Cache eviction handled by annotation
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.PRODUCTS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.CATEGORY_PRODUCTS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.SEARCH_PRODUCTS_CACHE, allEntries = true)
    })
    public void evictAllProductCaches() {
        // Cache eviction handled by annotation
    }

    @Override
    @CachePut(value = CacheConfig.PRODUCT_CACHE, key = "#product.id")
    public ProductResponse updateCachedProduct(Product product) {
        return productMapper.toDtoForCache(product);
    }
} 