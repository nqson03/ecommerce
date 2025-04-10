package com.ecommerce.mapper;

import com.ecommerce.dto.UserDto;
import com.ecommerce.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    @Autowired
    private EntityMapper entityMapper;
    
    /**
     * Chuyển đổi User thành UserDto
     */
    public UserDto toDto(User user) {
        return entityMapper.convertToDto(user, UserDto.class);
    }
    
    /**
     * Chuyển đổi UserDto thành User
     */
    public User toEntity(UserDto userDto) {
        return entityMapper.convertToEntity(userDto, User.class);
    }
    
    /**
     * Cập nhật User từ UserDto
     */
    public void updateUserFromDto(UserDto userDto, User user) {
        // Không cập nhật password từ DTO
        String currentPassword = user.getPassword();
        entityMapper.updateEntityFromDto(userDto, user);
        user.setPassword(currentPassword);
    }
}