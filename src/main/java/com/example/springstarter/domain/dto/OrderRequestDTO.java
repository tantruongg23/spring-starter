package com.example.springstarter.domain.dto;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class OrderRequestDTO {
    private UUID userId;
    // Map of ProductId -> Quantity
    private Map<UUID, Integer> productQuantities;
}
