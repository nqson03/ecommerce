package com.ecommerce.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

public class CustomUserDetails extends org.springframework.security.core.userdetails.User {
    
    private final Long id;
    
    public CustomUserDetails(Long id, String username, String password, 
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }
    
    public Long getId() {
        return id;
    }
}