package com.example.springstarter.mapper;

import com.example.springstarter.domain.dto.ProductDTO;
import com.example.springstarter.domain.entity.Product;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryMapper categoryMapper;

    public ProductDTO toDto(Product entity) {
        if (entity == null) return null;
        return ProductDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .stockQuantity(entity.getStockQuantity())
                .createdAt(entity.getCreatedAt())
                .categories(entity.getCategories() != null ? 
                    entity.getCategories().stream().map(categoryMapper::toDto).collect(Collectors.toSet()) : null)
                .build();
    }
}
