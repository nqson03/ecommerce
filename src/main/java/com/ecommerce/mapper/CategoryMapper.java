package com.ecommerce.mapper;

import com.ecommerce.dto.CategoryDto;
import com.ecommerce.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    @Autowired
    private EntityMapper entityMapper;

    /**
     * Chuyển đổi Category thành CategoryDto
     */
    public CategoryDto toDto(Category category) {
        CategoryDto dto = entityMapper.convertToDto(category, CategoryDto.class);            
        return dto;
    }
    
    /**
     * Chuyển đổi danh sách Category thành danh sách CategoryDto
     */
    public List<CategoryDto> toDtoList(List<Category> categories) {
        if (categories == null) {
            return new ArrayList<>();
        }
        return categories.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Chuyển đổi CategoryDto thành Category
     */
    public Category toEntity(CategoryDto dto) {
        if (dto == null) {
            return null;
        }
        
        // Tạo entity mới thay vì sử dụng ModelMapper trực tiếp
        Category category = new Category();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());
        
        // Không xử lý parent và subcategories ở đây vì cần truy vấn database
        // Các trường này sẽ được xử lý ở service layer
        
        return category;
    }
}