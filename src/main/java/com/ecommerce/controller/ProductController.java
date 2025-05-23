package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.service.interfaces.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
// Using fully qualified name for Swagger ApiResponse to avoid collision with DTO ApiResponse
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "API quản lý sản phẩm")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(summary = "Lấy tất cả sản phẩm", description = "Trả về danh sách tất cả sản phẩm có phân trang")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách sản phẩm thành công",
                content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    @RateLimit(authenticatedLimit = 100, anonymousLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @Parameter(description = "Thông tin phân trang") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", productService.getAllProducts(pageable)));
    }

    @Operation(summary = "Lấy sản phẩm theo ID", description = "Trả về thông tin chi tiết của sản phẩm theo ID")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin sản phẩm thành công",
                content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm",
                content = @Content)
    })
    @GetMapping("/{id}")
    @RateLimit(authenticatedLimit = 100, anonymousLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "ID của sản phẩm", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", productService.getProductById(id)));
    }

    @Operation(summary = "Lấy sản phẩm theo danh mục", description = "Trả về danh sách sản phẩm theo danh mục có phân trang")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách sản phẩm thành công",
                content = @Content(schema = @Schema(implementation = Page.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục",
                content = @Content)
    })
    @GetMapping("/category/{categoryId}")
    @RateLimit(authenticatedLimit = 100, anonymousLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByCategory(
            @Parameter(description = "ID của danh mục", required = true) @PathVariable Long categoryId, 
            @Parameter(description = "Thông tin phân trang") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully",productService.getProductsByCategory(categoryId, pageable)));
    }

    @Operation(summary = "Tìm kiếm sản phẩm", description = "Tìm kiếm sản phẩm theo từ khóa có phân trang")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm kiếm sản phẩm thành công",
                content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/search")
    @RateLimit(authenticatedLimit = 50, anonymousLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @Parameter(description = "Từ khóa tìm kiếm", required = true) @RequestParam String keyword, 
            @Parameter(description = "Thông tin phân trang") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully",productService.searchProducts(keyword, pageable)));
    }

    @Operation(summary = "Tạo sản phẩm mới", description = "Tạo sản phẩm mới trong hệ thống", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo sản phẩm thành công",
                content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Parameter(description = "Thông tin sản phẩm", required = true) @Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.ok(ApiResponse.success("Product created successfully",productService.createProduct(productRequest)));
    }

    @Operation(summary = "Cập nhật sản phẩm", description = "Cập nhật thông tin sản phẩm hiện có", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật sản phẩm thành công",
                content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm",
                content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "ID của sản phẩm", required = true) @PathVariable Long id, 
            @Parameter(description = "Thông tin sản phẩm cập nhật", required = true) @Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully",productService.updateProduct(id, productRequest)));
    }

    @Operation(summary = "Xóa sản phẩm", description = "Xóa sản phẩm hiện có", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa sản phẩm thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm",
                content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> deleteProduct(
            @Parameter(description = "ID của sản phẩm", required = true) @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully",null));
    }
}