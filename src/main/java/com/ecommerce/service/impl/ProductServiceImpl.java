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

    // Helper method to populate stock information
    private void populateStockInformation(ProductResponse productResponse) {
        if (productResponse == null || productResponse.getId() == null) return;
        Integer reservedStock = stockReservationService.getTotalReservedQuantityForProduct(productResponse.getId());
        // productResponse.getStock() should contain the actual physical stock from the database (set by toDtoForCache)
        Integer availableStock = productResponse.getStock() - reservedStock;
        
        productResponse.setReservedStock(reservedStock);
        productResponse.setAvailableStock(availableStock);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<ProductResponse> staticProductsPage = productCacheService.getStaticProducts(pageable);
        staticProductsPage.getContent().forEach(this::populateStockInformation);
        return staticProductsPage;
    }

    @Override
    public ProductResponse getProductById(Long id) {
        ProductResponse response = productCacheService.getStaticProduct(id);
        populateStockInformation(response);
        return response;
    }

    @Override
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<ProductResponse> staticProductsPage = productCacheService.getStaticProductsByCategory(categoryId, pageable);
        staticProductsPage.getContent().forEach(this::populateStockInformation);
        return staticProductsPage;
    }

    @Override
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        Page<ProductResponse> staticProductsPage = productCacheService.getStaticSearchProducts(keyword, pageable);
        staticProductsPage.getContent().forEach(this::populateStockInformation);
        return staticProductsPage;
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
        product.setStock(productRequest.getStock()); // Initial physical stock
        product.setImages(productRequest.getImages());
        product.setCategory(category);
        product.setSeller(currentUser);
        product.setAverageRating(0.0);
        
        Product savedProduct = productRepository.save(product);
        productCacheService.evictAllProductCaches(); // Evict list caches
        
        // Convert to DTO (static data) and then populate stock
        ProductResponse response = productMapper.toDtoForCache(savedProduct);
        populateStockInformation(response); // Populate real-time stock for the new product
        return response;
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
        product.setStock(productRequest.getStock()); // Updated physical stock
        product.setImages(productRequest.getImages());
        product.setCategory(category);
        
        Product updatedProduct = productRepository.save(product);
        
        productCacheService.updateCachedProduct(updatedProduct); 
        productCacheService.evictAllProductCaches(); 
        
        ProductResponse response = productMapper.toDtoForCache(updatedProduct);
        populateStockInformation(response);
        return response;
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
        
        productCacheService.evictProductCache(id); // Evict specific product cache
        productCacheService.evictAllProductCaches(); // Evict list caches
    }
} 