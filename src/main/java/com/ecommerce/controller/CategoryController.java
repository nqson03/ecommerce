package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.CategoryDto;
import com.ecommerce.dto.CategoryRequest;
import com.ecommerce.service.CategoryService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "API quản lý danh mục sản phẩm")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "Lấy tất cả danh mục", description = "Trả về danh sách tất cả danh mục sản phẩm")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách danh mục thành công",
                content = @Content(schema = @Schema(implementation = List.class)))
    })
    @GetMapping
    @RateLimit(authenticatedLimit = 100, anonymousLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllCategories() {
        List<CategoryDto> categories = categoryService.getAllCategories();
        ApiResponse<List<CategoryDto>> response = ApiResponse.success("Categories retrieved successfully",categories);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lấy danh mục theo ID", description = "Trả về thông tin chi tiết của danh mục theo ID")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin danh mục thành công",
                content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục",
                content = @Content)
    })
    @GetMapping("/{id}")
    @RateLimit(authenticatedLimit = 100, anonymousLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryById(
            @Parameter(description = "ID của danh mục", required = true) @PathVariable Long id) {
        CategoryDto category = categoryService.getCategoryById(id);
        ApiResponse<CategoryDto> response = ApiResponse.success("Category retrieved successfully",category);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Tạo danh mục mới", description = "Tạo danh mục sản phẩm mới", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo danh mục thành công",
                content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @Parameter(description = "Thông tin danh mục", required = true) @Valid @RequestBody CategoryRequest categoryRequest) {
        CategoryDto createdCategory = categoryService.createCategory(categoryRequest);
        ApiResponse<CategoryDto> response = ApiResponse.success("Category created successfully",createdCategory);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cập nhật danh mục", description = "Cập nhật thông tin danh mục hiện có", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật danh mục thành công",
                content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục",
                content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @Parameter(description = "ID của danh mục", required = true) @PathVariable Long id, 
            @Parameter(description = "Thông tin danh mục cập nhật", required = true) @Valid @RequestBody CategoryRequest categoryRequest) {
        CategoryDto updatedCategory = categoryService.updateCategory(id, categoryRequest);
        ApiResponse<CategoryDto> response = ApiResponse.success("Category updated successfully",updatedCategory);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa danh mục", description = "Xóa danh mục hiện có", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa danh mục thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục",
                content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<?>> deleteCategory(
            @Parameter(description = "ID của danh mục", required = true) @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}