package com.ecommerce.service.interfaces;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    
    Page<ProductResponse> getAllProducts(Pageable pageable);
    
    ProductResponse getProductById(Long id);
    
    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);
    
    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);
    
    ProductResponse createProduct(ProductRequest productRequest);
    
    ProductResponse updateProduct(Long id, ProductRequest productRequest);
    
    void deleteProduct(Long id);
} 