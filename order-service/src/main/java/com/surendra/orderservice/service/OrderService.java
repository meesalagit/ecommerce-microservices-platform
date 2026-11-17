package com.surendra.orderservice.service;

import com.surendra.orderservice.dto.OrderRequestDTO;
import com.surendra.orderservice.dto.OrderResponseDTO;
import com.surendra.orderservice.dto.ProductResponseDTO;
import com.surendra.orderservice.dto.UserResponseDTO;
import com.surendra.orderservice.entity.Order;
import com.surendra.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestClient restClient;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Value("${services.user-url}")
    private String userServiceUrl;

    @Value("${services.product-url}")
    private String productServiceUrl;

    @Value("${services.inventory-url}")
    private String inventoryServiceUrl;

    public OrderService(
            OrderRepository orderRepository,
            RestClient restClient
    ) {
        this.orderRepository = orderRepository;
        this.restClient = restClient;
    }

    public OrderResponseDTO createOrder(
            String authorizationHeader,
            OrderRequestDTO requestDTO
    ) {

        // Get currently authenticated user
        UserResponseDTO userResponseDTO = restClient.get()
                .uri(userServiceUrl + "/users/me")
                .header("Authorization", authorizationHeader)
                .retrieve()
                .body(UserResponseDTO.class);

        // Get product details
        ProductResponseDTO productResponseDTO = restClient.get()
                .uri(
                        productServiceUrl + "/products/{id}",
                        requestDTO.getProductId()
                )
                .retrieve()
                .body(ProductResponseDTO.class);

        // Check whether enough stock is available
        Boolean stockAvailable = restClient.get()
                .uri(
                        inventoryServiceUrl + "/inventory/{productId}/available?quantity={quantity}",
                        requestDTO.getProductId(),
                        requestDTO.getQuantity()
                )
                .header(
                        "X-Internal-Service-Key",
                        internalServiceKey
                )
                .retrieve()
                .body(Boolean.class);

        if (Boolean.FALSE.equals(stockAvailable)) {
            throw new RuntimeException(
                    "Insufficient stock for product ID: "
                            + requestDTO.getProductId()
            );
        }

        // Reduce inventory
        restClient.put()
                .uri(
                        inventoryServiceUrl + "/inventory/{productId}/reduce?quantity={quantity}",
                        requestDTO.getProductId(),
                        requestDTO.getQuantity()
                )
                .header(
                        "X-Internal-Service-Key",
                        internalServiceKey
                )
                .retrieve()
                .toBodilessEntity();

        // Calculate total order amount
        BigDecimal totalAmount = productResponseDTO.getPrice()
                .multiply(
                        BigDecimal.valueOf(requestDTO.getQuantity())
                );

        // Create order entity
        Order order = new Order();

        order.setUserId(userResponseDTO.getId());
        order.setProductId(requestDTO.getProductId());
        order.setQuantity(requestDTO.getQuantity());
        order.setProductPrice(productResponseDTO.getPrice());
        order.setTotalAmount(totalAmount);
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder;

        try {

            savedOrder = orderRepository.saveAndFlush(order);

        } catch (Exception exception) {

            // Compensation:
            // restore inventory if saving the order fails
            restClient.put()
                    .uri(
                            inventoryServiceUrl + "/inventory/{productId}/restore?quantity={quantity}",
                            requestDTO.getProductId(),
                            requestDTO.getQuantity()
                    )
                    .header(
                            "X-Internal-Service-Key",
                            internalServiceKey
                    )
                    .retrieve()
                    .toBodilessEntity();

            throw new RuntimeException(
                    "Order creation failed. Inventory was restored.",
                    exception
            );
        }

        // Convert saved entity to response DTO
        OrderResponseDTO responseDTO = new OrderResponseDTO();

        responseDTO.setId(savedOrder.getId());
        responseDTO.setUserId(savedOrder.getUserId());
        responseDTO.setProductId(savedOrder.getProductId());
        responseDTO.setQuantity(savedOrder.getQuantity());
        responseDTO.setProductPrice(savedOrder.getProductPrice());
        responseDTO.setTotalAmount(savedOrder.getTotalAmount());
        responseDTO.setStatus(savedOrder.getStatus());
        responseDTO.setCreatedAt(savedOrder.getCreatedAt());

        return responseDTO;
    }
}