package com.example.springstarter.service;

import com.example.springstarter.mapper.ProductMapper;
import com.example.springstarter.domain.dto.ProductDTO;
import com.example.springstarter.domain.entity.Product;
import com.example.springstarter.exception.ResourceNotFoundException;
import com.example.springstarter.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(UUID id) {
        return productRepository.findById(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Transactional
    public ProductDTO createProduct(Product product) {
        Product saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }
}
