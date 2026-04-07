package com.example.springstarter.mapper;

import com.example.springstarter.domain.dto.UserDTO;
import com.example.springstarter.domain.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toDto(User entity) {
        if (entity == null) return null;
        return UserDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .version(entity.getVersion())
                .build();
    }
}
