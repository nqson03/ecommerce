package com.ecommerce.service;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.CartItem;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserService userService;

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productMapper.toDtoPage(productRepository.findAll(pageable));
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toDto(product);
    }

    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productMapper.toDtoPage(productRepository.findByCategoryId(categoryId, pageable));
    }

    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productMapper.toDtoPage(productRepository.search(keyword, pageable));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        User currentUser = userService.getCurrentUser();
        
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));
        
        Product product = new Product();
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStock(productRequest.getStock());
        product.setImages(productRequest.getImages());
        product.setCategory(category);
        product.setSeller(currentUser);
        product.setAverageRating(0.0);
        
        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        User currentUser = userService.getCurrentUser();
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Check if the current user is the seller of the product
        if (!product.getSeller().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new RuntimeException("You don't have permission to update this product");
        }
        
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));
        
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStock(productRequest.getStock());
        product.setImages(productRequest.getImages());
        product.setCategory(category);
        
        Product updatedProduct = productRepository.save(product);
        return productMapper.toDto(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        User currentUser = userService.getCurrentUser();
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Check if the current user is the seller of the product
        if (!product.getSeller().getId().equals(currentUser.getId()) && 
                !currentUser.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new RuntimeException("You don't have permission to delete this product");
        }
        
        productRepository.delete(product);
    }

        /**
     * Kiểm tra xem có đủ số lượng sản phẩm trong kho không
     * @param cartItems Danh sách sản phẩm trong giỏ hàng
     * @throws RuntimeException nếu không đủ số lượng sản phẩm
     */
    public void checkStockAvailability(List<CartItem> cartItems) {
        for (var cartItem : cartItems) {
            Product product = cartItem.getProduct();
            Integer requestedQuantity = cartItem.getQuantity();
            
            if (product.getStock() < requestedQuantity) {
                throw new RuntimeException("Insufficient quantity of product " + product.getName() +
                    " in stock. Only " + product.getStock() + " items remaining.");
            }
        }
    }
    
    /**
     * Cập nhật số lượng sản phẩm trong kho sau khi tạo đơn hàng
     * @param orderItems Danh sách sản phẩm trong đơn hàng
     */
    public void updateProductStock(List<OrderItem> orderItems) {
        for (var orderItem : orderItems) {
            Product product = orderItem.getProduct();
            Integer orderedQuantity = orderItem.getQuantity();
            
            // Trừ số lượng đã đặt từ stock hiện có
            product.setStock(product.getStock() - orderedQuantity);
            
            // Lưu lại thông tin sản phẩm đã cập nhật vào database
            productRepository.save(product);
        }
    }
    
    /**
     * Khôi phục số lượng sản phẩm trong kho khi hủy đơn hàng
     * @param orderItems Danh sách sản phẩm trong đơn hàng
     */
    public void restoreProductStock(List<OrderItem> orderItems) {
        for (var orderItem : orderItems) {
            Product product = orderItem.getProduct();
            Integer orderedQuantity = orderItem.getQuantity();
            
            // Cộng lại số lượng đã đặt vào stock hiện có
            product.setStock(product.getStock() + orderedQuantity);
            
            // Lưu lại thông tin sản phẩm đã cập nhật vào database
            productRepository.save(product);
        }
    }
    // private ProductResponse convertToProductResponse(Product product) {
    //     ProductResponse response = new ProductResponse();
    //     response.setId(product.getId());
    //     response.setName(product.getName());
    //     response.setDescription(product.getDescription());
    //     response.setPrice(product.getPrice());
    //     response.setStock(product.getStock());
    //     response.setImages(product.getImages());
    //     response.setAverageRating(product.getAverageRating());
    //     response.setCreatedAt(product.getCreatedAt());
    //     response.setUpdatedAt(product.getUpdatedAt());
        
    //     ProductResponse.CategoryDto categoryDto = new ProductResponse.CategoryDto();
    //     categoryDto.setId(product.getCategory().getId());
    //     categoryDto.setName(product.getCategory().getName());
    //     response.setCategory(categoryDto);
        
    //     ProductResponse.UserSummaryDto sellerDto = new ProductResponse.UserSummaryDto();
    //     sellerDto.setId(product.getSeller().getId());
    //     sellerDto.setUsername(product.getSeller().getUsername());
    //     sellerDto.setFullName(product.getSeller().getFullName());
    //     response.setSeller(sellerDto);
        
    //     return response;
    // }
}