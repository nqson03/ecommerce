package com.ecommerce.service.interfaces;

import com.ecommerce.dto.CategoryDto;
import com.ecommerce.dto.CategoryRequest;

import java.util.List;

public interface CategoryService {
    
    List<CategoryDto> getAllCategories();
    
    CategoryDto getCategoryById(Long id);
    
    CategoryDto createCategory(CategoryRequest categoryRequest);
    
    CategoryDto updateCategory(Long id, CategoryRequest categoryRequest);
    
    void deleteCategory(Long id);
} 