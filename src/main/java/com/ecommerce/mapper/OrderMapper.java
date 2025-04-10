package com.ecommerce.mapper;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.dto.OrderItemDto;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    @Autowired
    private EntityMapper entityMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * Chuyển đổi Order thành OrderDto
     */
    public OrderDto toDto(Order order) {
        OrderDto dto = entityMapper.convertToDto(order, OrderDto.class);
        
        // Xử lý user
        if (order.getUser() != null) {
            dto.setUser(userMapper.toDto(order.getUser()));
        }
        
        // Xử lý items
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            dto.setItems(order.getItems().stream()
                    .map(this::convertOrderItemToDto)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    /**
     * Chuyển đổi OrderItem thành OrderItemDto
     */
    private OrderItemDto convertOrderItemToDto(OrderItem orderItem) {
        OrderItemDto dto = entityMapper.convertToDto(orderItem, OrderItemDto.class);
        
        if (orderItem.getOrder() != null) {
            dto.setOrderId(orderItem.getOrder().getId());
        }
        
        if (orderItem.getProduct() != null) {
            dto.setProductId(orderItem.getProduct().getId());
            dto.setProductName(orderItem.getProduct().getName());
            if (orderItem.getProduct().getImages() != null && !orderItem.getProduct().getImages().isEmpty()) {
                dto.setProductImage(orderItem.getProduct().getImages().get(0));
            }
        }
        
        // Tính tổng giá
        if (orderItem.getPrice() != null && orderItem.getQuantity() != null) {
            dto.setTotalPrice(orderItem.getPrice().multiply(new BigDecimal(orderItem.getQuantity())));
        }
        
        return dto;
    }
    
    /**
     * Chuyển đổi Page<Order> thành Page<OrderDto>
     */
    public Page<OrderDto> toDtoPage(Page<Order> orders) {
        return orders.map(this::toDto);
    }
}