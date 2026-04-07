package com.example.springstarter.mapper;

import com.example.springstarter.domain.dto.OrderDTO;
import com.example.springstarter.domain.dto.OrderItemDTO;
import com.example.springstarter.domain.entity.Order;
import com.example.springstarter.domain.entity.OrderItem;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderItemDTO toOrderItemDto(OrderItem entity) {
        if (entity == null) return null;
        return OrderItemDTO.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .productName(entity.getProduct() != null ? entity.getProduct().getName() : null)
                .quantity(entity.getQuantity())
                .priceAtPurchase(entity.getPriceAtPurchase())
                .build();
    }

    public OrderDTO toDto(Order entity) {
        if (entity == null) return null;
        return OrderDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .createdAt(entity.getCreatedAt())
                .items(entity.getItems() != null ? 
                    entity.getItems().stream().map(this::toOrderItemDto).collect(Collectors.toList()) : null)
                .build();
    }
}
