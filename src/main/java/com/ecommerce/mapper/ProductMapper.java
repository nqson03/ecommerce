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
     * Chuyển đổi Product thành ProductResponse
     */
    public ProductResponse toDto(Product product) {
        ProductResponse response = entityMapper.convertToDto(product, ProductResponse.class);
        
        // Xử lý các trường đặc biệt nếu cần
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
        
        return response;
    }
    
    /**
     * Chuyển đổi Page<Product> thành Page<ProductResponse>
     */
    public Page<ProductResponse> toDtoPage(Page<Product> products) {
        return products.map(this::toDto);
    }
}