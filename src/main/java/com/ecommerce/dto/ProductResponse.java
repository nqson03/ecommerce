package com.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private List<String> images;
    private CategoryDto category;
    private UserSummaryDto seller;
    private Double averageRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    public static class CategoryDto {
        private Long id;
        private String name;
    }
    
    @Data
    public static class UserSummaryDto {
        private Long id;
        private String username;
        private String fullName;
    }
}