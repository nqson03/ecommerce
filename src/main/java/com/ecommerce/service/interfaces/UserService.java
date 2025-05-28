package com.ecommerce.service.interfaces;

import com.ecommerce.dto.UserDto;
import com.ecommerce.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    
    User getCurrentUser();
    
    User getUserById(Long userId);
    
    UserDto getCurrentUserInfo(long id);
    
    UserDto updateCurrentUser(UserDto userDto);
    
    void changePassword(String currentPassword, String newPassword);
    
    void deleteCurrentUser(long id, String password);
    
    Page<UserDto> getAllUsers(Pageable pageable);
    
    UserDto updateUserRole(Long userId, User.Role role);
} 