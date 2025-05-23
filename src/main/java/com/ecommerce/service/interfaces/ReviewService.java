package com.ecommerce.service.interfaces;

import com.ecommerce.dto.ReviewRequest;
import com.ecommerce.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    
    Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable);
    
    ReviewResponse createReview(ReviewRequest reviewRequest);
    
    ReviewResponse updateReview(Long id, ReviewRequest reviewRequest);
    
    void deleteReview(Long id);
} 