package com.ecommerce.service.impl;

import com.ecommerce.config.CacheConfig;
import com.ecommerce.dto.UserDto;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
    
    @Cacheable(value = CacheConfig.USER_CACHE, key = "#id")
    public UserDto getCurrentUserInfo(long id) {
        User user = getCurrentUser();
        return userMapper.toDto(user);
    }
    
    @Transactional
    @Caching(
        put = { @CachePut(value = CacheConfig.USER_CACHE, key = "#result.id") },
        evict = { @CacheEvict(value = CacheConfig.USERS_CACHE, allEntries = true) }
    )
    public UserDto updateCurrentUser(UserDto userDto) {
        User currentUser = getCurrentUser();
        
        // Không cho phép thay đổi id, username và role qua endpoint này
        userDto.setId(currentUser.getId());
        userDto.setUsername(currentUser.getUsername());
        userDto.setRole(currentUser.getRole());
        
        userMapper.updateUserFromDto(userDto, currentUser);
        User updatedUser = userRepository.save(currentUser);
        
        return userMapper.toDto(updatedUser);
    }
    
    @Transactional
    public void changePassword(long id, String currentPassword, String newPassword) {
        User user = getCurrentUser();
        
        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AccessDeniedException("Current password is incorrect");
        }
        
        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = CacheConfig.USER_CACHE, key = "#id"),
            @CacheEvict(value = CacheConfig.USERS_CACHE, allEntries = true)
        }
    )
    public void deleteCurrentUser(long id, String password) {
        User user = getCurrentUser();
        
        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AccessDeniedException("Password is incorrect");
        }
        
        userRepository.delete(user);
    }
    
    // Admin functions
    @Cacheable(value = CacheConfig.USERS_CACHE, key = "#pageable.toString()")
    public Page<UserDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toDto);
    }
    
    @Transactional
    @Caching(
        put = { @CachePut(value = CacheConfig.USER_CACHE, key = "#result.id") },
        evict = { @CacheEvict(value = CacheConfig.USERS_CACHE, allEntries = true) }
    )
    public UserDto updateUserRole(Long userId, User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        
        return userMapper.toDto(updatedUser);
    }
} 