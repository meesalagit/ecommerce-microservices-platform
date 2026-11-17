package com.surendra.orderservice.controller;

import com.surendra.orderservice.dto.OrderRequestDTO;
import com.surendra.orderservice.dto.OrderResponseDTO;
import com.surendra.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody OrderRequestDTO requestDTO
    ) {
        OrderResponseDTO responseDTO =
                orderService.createOrder(
                        authorizationHeader,
                        requestDTO
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseDTO);
    }
}