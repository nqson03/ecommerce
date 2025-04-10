package com.ecommerce.dto;

import com.ecommerce.model.User;
import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String profilePicture;
    private User.Role role;
}