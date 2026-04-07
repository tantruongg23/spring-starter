package com.example.springstarter.service;

import com.example.springstarter.mapper.UserMapper;
import com.example.springstarter.domain.dto.UserDTO;
import com.example.springstarter.domain.dto.UserRequestDTO;
import com.example.springstarter.domain.entity.User;
import com.example.springstarter.exception.ResourceNotFoundException;
import com.example.springstarter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserDTO getUserById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional
    public UserDTO createUser(UserRequestDTO userRequestDTO) {
        if (userRequestDTO.getId() != null) {
            User existing = userRepository.findById(userRequestDTO.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            existing.setUsername(userRequestDTO.getUsername());
            existing.setEmail(userRequestDTO.getEmail());
            existing.setRole(userRequestDTO.getRole());
            if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
            }
            return userMapper.toDto(userRepository.save(existing));
        }

        User user = new User();
        user.setUsername(userRequestDTO.getUsername());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        user.setRole(userRequestDTO.getRole());
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }
}
