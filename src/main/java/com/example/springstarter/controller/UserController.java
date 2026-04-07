package com.example.springstarter.controller;

import com.example.springstarter.domain.dto.UserDTO;
import com.example.springstarter.domain.dto.UserRequestDTO;
import com.example.springstarter.response.ApiResponse;
import com.example.springstarter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDTO> createUser(@RequestBody UserRequestDTO userRequestDTO) {
        UserDTO createdUser = userService.createUser(userRequestDTO);
        return ApiResponse.<UserDTO>builder().status(200).data(
                createdUser
        ).message("Created user successfully!").build();
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDTO> getUser(@PathVariable UUID id){
        return ApiResponse.<UserDTO>builder().status(200).data(
                userService.getUserById(id)
        ).message("Fetched user successfully!").build();
    }
}
