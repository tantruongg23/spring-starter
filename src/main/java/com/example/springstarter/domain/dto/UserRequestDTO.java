package com.example.springstarter.domain.dto;

import com.example.springstarter.domain.enumerate.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {
    private UUID id;
    private String username;
    private String password;
    private String email;
    private Role role;
}
