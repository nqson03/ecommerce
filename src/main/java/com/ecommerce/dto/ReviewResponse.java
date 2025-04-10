package com.ecommerce.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private UserSummary user;
    private Integer rating;
    private String comment;
    private List<String> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    public static class UserSummary {
        private Long id;
        private String username;
        private String avatarUrl;
    }
}