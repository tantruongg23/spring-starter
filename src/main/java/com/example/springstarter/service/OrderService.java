package com.example.springstarter.service;

import com.example.springstarter.mapper.OrderMapper;
import com.example.springstarter.domain.dto.OrderDTO;
import com.example.springstarter.domain.entity.Order;
import com.example.springstarter.domain.entity.OrderItem;
import com.example.springstarter.domain.entity.Product;
import com.example.springstarter.domain.entity.User;
import com.example.springstarter.domain.enumerate.OrderStatus;
import com.example.springstarter.exception.BusinessException;
import com.example.springstarter.exception.ResourceNotFoundException;
import com.example.springstarter.repository.OrderRepository;
import com.example.springstarter.repository.ProductRepository;
import com.example.springstarter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public List<OrderDTO> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO createOrder(UUID userId, Map<UUID, Integer> productQuantities) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<UUID, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", entry.getKey()));
            
            int quantity = entry.getValue();
            if (product.getStockQuantity() < quantity) {
                throw new BusinessException("Insufficient stock for product " + product.getName());
            }

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .priceAtPurchase(product.getPrice())
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        return orderMapper.toDto(order);
    }
}
