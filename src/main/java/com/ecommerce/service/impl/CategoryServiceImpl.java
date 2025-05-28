package com.ecommerce.service.impl;

import com.ecommerce.config.CacheConfig;
import com.ecommerce.dto.CategoryRequest;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.dto.CategoryDto;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.interfaces.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryMapper categoryMapper;

    @Cacheable(value = CacheConfig.CATEGORIES_CACHE)
    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
               .map(categoryMapper::toDto)
               .toList();
    }

    @Cacheable(value = CacheConfig.CATEGORY_CACHE, key = "#id")
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
               .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return categoryMapper.toDto(category);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CATEGORIES_CACHE}, allEntries = true)
    public CategoryDto createCategory(CategoryRequest categoryRequest) {
        Category category = new Category();
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        category.setImageUrl(categoryRequest.getImageUrl());
        
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    @Caching(
        put = { @CachePut(value = CacheConfig.CATEGORY_CACHE, key = "#result.id") },
        evict = {
            @CacheEvict(value = CacheConfig.CATEGORIES_CACHE, allEntries = true)
        }
    )
    public CategoryDto updateCategory(Long id, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        category.setImageUrl(categoryRequest.getImageUrl());
        
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CATEGORY_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.CATEGORIES_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.CATEGORY_PRODUCTS_CACHE, allEntries = true)
    })
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        categoryRepository.delete(category);
    }
} 