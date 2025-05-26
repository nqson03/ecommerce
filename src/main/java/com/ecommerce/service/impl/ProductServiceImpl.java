package com.ecommerce.service.impl;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedOperationException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.interfaces.ProductCacheService;
import com.ecommerce.service.interfaces.ProductService;
import com.ecommerce.service.interfaces.StockReservationService;
import com.ecommerce.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ARCHITECTURE:
 * - Cache Layer: ProductCacheService handles static data caching
 * - Business Layer: ProductService handles business logic + real-time stock
 * 
 * Pattern:
 * ProductCacheService.getStaticXxx() → Cache static data only
 * ProductService.getXxx() → Static data + Real-time stock = Complete response
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserService userService;
    
    @Autowired
    private StockReservationService stockReservationService;
    
    @Autowired
    private ProductCacheService productCacheService;

    // =============== BUSINESS LOGIC WITH REAL-TIME STOCK ===============

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<ProductResponse> staticProducts = productCacheService.getStaticProducts(pageable);
        
        // Populate real-time stock info cho từng product
        staticProducts.getContent().forEach(response -> 
            productMapper.populateStockInfo(response, response.getId())
        );
        
        return staticProducts;
    }

    @Override
    public ProductResponse getProductById(Long id) {
        ProductResponse response = productCacheService.getStaticProduct(id);
        productMapper.populateStockInfo(response, id);         // luôn realtime
        return response;
    }

    @Override
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<ProductResponse> staticProducts = productCacheService.getStaticProductsByCategory(categoryId, pageable);
        
        // Populate real-time stock info cho từng product
        staticProducts.getContent().forEach(response -> 
            productMapper.populateStockInfo(response, response.getId())
        );
        
        return staticProducts;
    }

    @Override
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        Page<ProductResponse> staticProducts = productCacheService.getStaticSearchProducts(keyword, pageable);
        
        // Populate real-time stock info cho từng product
        staticProducts.getContent().forEach(response -> 
            productMapper.populateStockInfo(response, response.getId())
        );
        
        return staticProducts;
    }

    // =============== CUD OPERATIONS ===============

    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        User currentUser = userService.getCurrentUser();
        
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));
        
        Product product = new Product();
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStock(productRequest.getStock());
        product.setImages(productRequest.getImages());
        product.setCategory(category);
        product.setSeller(currentUser);
        product.setAverageRating(0.0);
        
        Product savedProduct = productRepository.save(product);
        
        // Evict caches after creating new product
        productCacheService.evictAllProductCaches();
        
        // Return với real-time stock cho newly created product
        return productMapper.toDto(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        User currentUser = userService.getCurrentUser();
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Check if the current user is the seller of the product
        if (!product.getSeller().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new RuntimeException("You don't have permission to update this product");
        }
        
        // Validate stock update - không cho set stock < reserved stock
        Integer currentReservedQuantity = stockReservationService.getTotalReservedQuantityForProduct(id);
        if (currentReservedQuantity == null) {
            currentReservedQuantity = 0;
        }
        
        if (productRequest.getStock() < currentReservedQuantity) {
            throw new InsufficientStockException(
                "Cannot set stock to " + productRequest.getStock() + 
                " because there are " + currentReservedQuantity + " items currently reserved. " +
                "Available stock would be negative. Please wait for reservations to expire or be confirmed."
            );
        }
        
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));
        
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStock(productRequest.getStock());
        product.setImages(productRequest.getImages());
        product.setCategory(category);
        
        Product updatedProduct = productRepository.save(product);
        
        // Evict caches after updating product
        productCacheService.evictProductCache(id);
        productCacheService.evictAllProductCaches();
        
        // Return với real-time stock cho updated product
        return productMapper.toDto(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        User currentUser = userService.getCurrentUser();
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Check if the current user is the seller of the product
        if (!product.getSeller().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new UnauthorizedOperationException("You don't have permission to delete this product");
        }
        
        // Validate không có active reservations
        Integer activeReservations = stockReservationService.getTotalReservedQuantityForProduct(id);
        if (activeReservations > 0) {
            throw new InsufficientStockException(
                "Cannot delete product because there are " + activeReservations + 
                " items currently reserved. Please wait for reservations to expire or be confirmed."
            );
        }
        
        productRepository.delete(product);
        
        // Evict caches after deleting product
        productCacheService.evictProductCache(id);
        productCacheService.evictAllProductCaches();
    }
} 