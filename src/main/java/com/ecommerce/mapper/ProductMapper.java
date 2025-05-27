package com.ecommerce.mapper;

import com.ecommerce.dto.ProductResponse;
import com.ecommerce.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    @Autowired
    private EntityMapper entityMapper;
    
    /**
     * Convert cho cache - KHÔNG bao gồm availableStock/reservedStock
     * Chỉ convert static data để tránh cache stale stock information
     */
    public ProductResponse toDtoForCache(Product product) {
        ProductResponse response = entityMapper.convertToDto(product, ProductResponse.class);
        
        // Xử lý các trường static
        if (product.getCategory() != null) {
            ProductResponse.CategoryDto categoryDto = new ProductResponse.CategoryDto();
            categoryDto.setId(product.getCategory().getId());
            categoryDto.setName(product.getCategory().getName());
            response.setCategory(categoryDto);
        }
        
        if (product.getSeller() != null) {
            ProductResponse.UserSummaryDto sellerDto = new ProductResponse.UserSummaryDto();
            sellerDto.setId(product.getSeller().getId());
            sellerDto.setUsername(product.getSeller().getUsername());
            sellerDto.setFullName(product.getSeller().getFullName());
            response.setSeller(sellerDto);
        }
        
        // KHÔNG set availableStock/reservedStock để tránh cache stale data
        response.setAvailableStock(null);
        response.setReservedStock(null);
        
        return response;
    }
    
    /**
     * Chuyển đổi Page<Product> cho cache - không có stock info
     */
    public Page<ProductResponse> toDtoPageForCache(Page<Product> products) {
        return products.map(this::toDtoForCache);
    }
}