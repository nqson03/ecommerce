package com.ecommerce.controller;

import com.ecommerce.annotation.RateLimit;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.model.Order;
import com.ecommerce.dto.OrderDto;
import com.ecommerce.model.User; 
import com.ecommerce.service.interfaces.OrderService;
import com.ecommerce.service.interfaces.UserService; 
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
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "API quản lý đơn hàng")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService; 

    @Operation(summary = "Lấy danh sách đơn hàng của người dùng", description = "Trả về danh sách đơn hàng của người dùng hiện tại", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách đơn hàng thành công",
                content = @Content(schema = @Schema(implementation = Page.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('USER')") 
    @RateLimit(authenticatedLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getUserOrders(
            @Parameter(description = "Thông tin phân trang") Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orderService.getUserOrders(currentUser.getId(), pageable)));
    }

    @Operation(summary = "Lấy thông tin đơn hàng theo ID", description = "Trả về thông tin chi tiết của đơn hàng theo ID", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin đơn hàng thành công",
                content = @Content(schema = @Schema(implementation = OrderDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng",
                content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") 
    @RateLimit(authenticatedLimit = 50, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(
            @Parameter(description = "ID của đơn hàng", required = true) @PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Get Order successfully", orderService.getOrderById(id, currentUser)));
    }

    @Operation(summary = "Tạo đơn hàng mới", description = "Tạo đơn hàng mới cho người dùng hiện tại", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo đơn hàng thành công",
                content = @Content(schema = @Schema(implementation = OrderDto.class))),
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
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @Parameter(description = "Thông tin đơn hàng", required = true) @Valid @RequestBody OrderRequest orderRequest) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", orderService.createOrder(orderRequest, currentUser)));
    }

    @Operation(summary = "Hủy đơn hàng", description = "Hủy đơn hàng hiện có", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Hủy đơn hàng thành công",
                content = @Content(schema = @Schema(implementation = OrderDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng",
                content = @Content)
    })
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") 
    @RateLimit(authenticatedLimit = 10, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @Parameter(description = "ID của đơn hàng", required = true) @PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Order canceled successfully", orderService.cancelOrder(id, currentUser)));
    }

    @Operation(summary = "Lấy tất cả đơn hàng (Admin)", description = "Trả về danh sách tất cả đơn hàng trong hệ thống", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách đơn hàng thành công",
                content = @Content(schema = @Schema(implementation = Page.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content)
    })
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 30, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getAllOrders(
            @Parameter(description = "Thông tin phân trang") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully",orderService.getAllOrders(pageable)));
    }

    @Operation(summary = "Cập nhật trạng thái đơn hàng (Admin)", description = "Cập nhật trạng thái của đơn hàng hiện có", 
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật trạng thái đơn hàng thành công",
                content = @Content(schema = @Schema(implementation = OrderDto.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng",
                content = @Content)
    })
    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(authenticatedLimit = 20, refreshPeriod = 60)
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @Parameter(description = "ID của đơn hàng", required = true) @PathVariable Long id, 
            @Parameter(description = "Trạng thái mới của đơn hàng", required = true) @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully",orderService.updateOrderStatus(id, null, status)));
    }
}