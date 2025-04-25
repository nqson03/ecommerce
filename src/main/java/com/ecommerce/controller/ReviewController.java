package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.ReviewRequest;
import com.ecommerce.dto.ReviewResponse;
import com.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/product/{productId}")
    @RateLimit(authenticatedLimit = 100, anonymousLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
            "Product reviews retrieved successfully",
            reviewService.getProductReviews(productId, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest reviewRequest) {
        return ResponseEntity.ok(ApiResponse.success(
            "Review created successfully",
            reviewService.createReview(reviewRequest)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long id, @Valid @RequestBody ReviewRequest reviewRequest) {
        return ResponseEntity.ok(ApiResponse.success(
            "Review updated successfully",
            reviewService.updateReview(id, reviewRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(authenticatedLimit = 5, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }
}