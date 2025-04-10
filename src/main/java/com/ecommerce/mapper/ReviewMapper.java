package com.ecommerce.mapper;

import com.ecommerce.dto.ReviewResponse;
import com.ecommerce.model.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    @Autowired
    private EntityMapper entityMapper;
    
    /**
     * Chuyển đổi Review thành ReviewResponse
     */
    public ReviewResponse toResponse(Review review) {
        ReviewResponse response = entityMapper.convertToDto(review, ReviewResponse.class);
        
        if (review.getProduct() != null) {
            response.setProductId(review.getProduct().getId());
            response.setProductName(review.getProduct().getName());
        }
        
        if (review.getUser() != null) {
            ReviewResponse.UserSummary userSummary = new ReviewResponse.UserSummary();
            userSummary.setId(review.getUser().getId());
            userSummary.setUsername(review.getUser().getUsername());
            userSummary.setAvatarUrl(review.getUser().getProfilePicture());
            response.setUser(userSummary);
        }
        
        return response;
    }
    
    /**
     * Chuyển đổi Page<Review> thành Page<ReviewResponse>
     */
    public Page<ReviewResponse> toResponsePage(Page<Review> reviews) {
        return reviews.map(this::toResponse);
    }

}