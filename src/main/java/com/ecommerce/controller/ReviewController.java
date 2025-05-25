package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.ReviewRequest;
import com.ecommerce.dto.ReviewResponse;
import com.ecommerce.service.interfaces.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Review", description = "API quản lý đánh giá sản phẩm")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Operation(summary = "Lấy danh sách đánh giá của sản phẩm", description = "Trả về danh sách đánh giá của sản phẩm theo ID", tags = {"Review"})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách đánh giá thành công",
                content = @Content(schema = @Schema(implementation = Page.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm",
                content = @Content)
    })
    @GetMapping("/product/{productId}")
    @RateLimit(authenticatedLimit = 100, anonymousLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @Parameter(description = "ID của sản phẩm", required = true) @PathVariable Long productId, 
            @Parameter(description = "Thông tin phân trang") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
            "Product reviews retrieved successfully",
            reviewService.getProductReviews(productId, pageable)));
    }

    @Operation(summary = "Tạo đánh giá mới", description = "Tạo đánh giá mới cho sản phẩm", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo đánh giá thành công",
                content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Parameter(description = "Thông tin đánh giá", required = true) @Valid @RequestBody ReviewRequest reviewRequest) {
        return ResponseEntity.ok(ApiResponse.success(
            "Review created successfully",
            reviewService.createReview(reviewRequest)));
    }

    @Operation(summary = "Cập nhật đánh giá", description = "Cập nhật đánh giá hiện có", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật đánh giá thành công",
                content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đánh giá",
                content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @Parameter(description = "ID của đánh giá", required = true) @PathVariable Long id, 
            @Parameter(description = "Thông tin đánh giá cập nhật", required = true) @Valid @RequestBody ReviewRequest reviewRequest) {
        return ResponseEntity.ok(ApiResponse.success(
            "Review updated successfully",
            reviewService.updateReview(id, reviewRequest)));
    }

    @Operation(summary = "Xóa đánh giá", description = "Xóa đánh giá hiện có", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa đánh giá thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đánh giá",
                content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(authenticatedLimit = 5, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @Parameter(description = "ID của đánh giá", required = true) @PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }
}