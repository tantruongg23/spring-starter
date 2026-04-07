package com.example.springstarter.domain.dto;

import com.example.springstarter.domain.enumerate.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String username;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    private int version;
}
