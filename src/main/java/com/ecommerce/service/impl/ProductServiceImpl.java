package com.ecommerce.service.impl;

import com.ecommerce.config.CacheConfig;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.interfaces.ProductService;
import com.ecommerce.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "#pageable.toString()")
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productMapper.toDtoPage(productRepository.findAll(pageable));
    }

    @Cacheable(value = CacheConfig.PRODUCT_CACHE, key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toDto(product);
    }

    @Cacheable(value = CacheConfig.CATEGORY_PRODUCTS_CACHE, key = "#categoryId + '_' + #pageable.toString()")
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productMapper.toDtoPage(productRepository.findByCategoryId(categoryId, pageable));
    }

    @Cacheable(value = CacheConfig.SEARCH_PRODUCTS_CACHE, key = "#keyword + '_' + #pageable.toString()")
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productMapper.toDtoPage(productRepository.search(keyword, pageable));
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.PRODUCTS_CACHE, CacheConfig.CATEGORY_PRODUCTS_CACHE, CacheConfig.SEARCH_PRODUCTS_CACHE}, allEntries = true)
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
        return productMapper.toDto(savedProduct);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#id"),
        @CacheEvict(value = {CacheConfig.PRODUCTS_CACHE, CacheConfig.CATEGORY_PRODUCTS_CACHE, CacheConfig.SEARCH_PRODUCTS_CACHE}, allEntries = true)
    })
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        User currentUser = userService.getCurrentUser();
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Check if the current user is the seller of the product
        if (!product.getSeller().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new RuntimeException("You don't have permission to update this product");
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
        return productMapper.toDto(updatedProduct);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#id"),
        @CacheEvict(value = {CacheConfig.PRODUCTS_CACHE, CacheConfig.CATEGORY_PRODUCTS_CACHE, CacheConfig.SEARCH_PRODUCTS_CACHE}, allEntries = true)
    })
    public void deleteProduct(Long id) {
        User currentUser = userService.getCurrentUser();
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Check if the current user is the seller of the product
        if (!product.getSeller().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new RuntimeException("You don't have permission to delete this product");
        }
        
        productRepository.delete(product);
    }
} 