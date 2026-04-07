package com.example.springstarter.controller;

import com.example.springstarter.domain.dto.OrderDTO;
import com.example.springstarter.domain.dto.OrderRequestDTO;
import com.example.springstarter.response.ApiResponse;
import com.example.springstarter.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByUserId(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@RequestBody OrderRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.createOrder(request.getUserId(), request.getProductQuantities()), "Order placed successfully"));
    }
}
