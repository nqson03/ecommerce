package com.ecommerce.service;

import com.ecommerce.config.CacheConfig;
import com.ecommerce.dto.ReviewRequest;
import com.ecommerce.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.ecommerce.dto.ReviewResponse;
import com.ecommerce.mapper.ReviewMapper;
import com.ecommerce.model.Product;
import com.ecommerce.model.Review;
import com.ecommerce.model.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private UserService userService;

    @Cacheable(value = CacheConfig.REVIEWS_CACHE, key = "#productId + '_' + #pageable.toString()")
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewMapper.toResponsePage(reviewRepository.findByProductId(productId, pageable));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.REVIEWS_CACHE, allEntries = true)
    public ReviewResponse createReview(ReviewRequest reviewRequest) {
        User currentUser = userService.getCurrentUser();
        
        Product product = productRepository.findById(reviewRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + reviewRequest.getProductId()));
        
        Review review = new Review();
        review.setUser(currentUser);
        review.setProduct(product);
        review.setRating(reviewRequest.getRating());
        review.setComment(reviewRequest.getComment());
        review.setImages(reviewRequest.getImages());
        
        Review savedReview = reviewRepository.save(review);
        
        // Update product average rating
        updateProductAverageRating(product);
        
        return reviewMapper.toResponse(savedReview);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.REVIEWS_CACHE, allEntries = true)
    public ReviewResponse updateReview(Long id, ReviewRequest reviewRequest) {
        User currentUser = userService.getCurrentUser();
        
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        // Check if the review belongs to the current user
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to update this review");
        }
        
        Product product = productRepository.findById(reviewRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + reviewRequest.getProductId()));
        
        review.setProduct(product);
        review.setRating(reviewRequest.getRating());
        review.setComment(reviewRequest.getComment());
        review.setImages(reviewRequest.getImages());
        
        Review updatedReview = reviewRepository.save(review);
        
        // Update product average rating
        updateProductAverageRating(product);
        
        return reviewMapper.toResponse(updatedReview);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.REVIEWS_CACHE, allEntries = true)
    public void deleteReview(Long id) {
        User currentUser = userService.getCurrentUser();
        
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        // Check if the review belongs to the current user
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this review");
        }
        
        Product product = review.getProduct();
        
        reviewRepository.delete(review);
        
        // Update product average rating
        updateProductAverageRating(product);
    }

    @Transactional
    private void updateProductAverageRating(Product product) {
        Double averageRating = reviewRepository.findAverageRatingByProductId(product.getId());
        product.setAverageRating(averageRating != null ? averageRating : 0.0);
        productRepository.save(product);
    }
}