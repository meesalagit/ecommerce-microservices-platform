package com.surendra.inventoryservice.controller;

import com.surendra.inventoryservice.dto.InventoryRequestDTO;
import com.surendra.inventoryservice.dto.InventoryResponseDTO;
import com.surendra.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/inventory")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponseDTO> createInventory(
            @Valid @RequestBody InventoryRequestDTO requestDTO
    ) {
        InventoryResponseDTO response =
                inventoryService.createInventory(requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponseDTO> getInventoryByProductId(
            @PathVariable Long productId
    ) {
        InventoryResponseDTO response =
                inventoryService.getInventoryByProductId(productId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponseDTO> updateInventory(
            @PathVariable Long productId,
            @RequestParam @Min(
                    value = 1,
                    message = "Quantity must be at least 1"
            ) Integer quantity
    ) {
        InventoryResponseDTO response =
                inventoryService.updateInventory(productId, quantity);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}/reduce")
    public ResponseEntity<InventoryResponseDTO> reduceStock(
            @PathVariable Long productId,
            @RequestParam
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity
    ) {
        InventoryResponseDTO response =
                inventoryService.reduceStock(productId, quantity);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}/restore")
    public ResponseEntity<InventoryResponseDTO> restoreStock(
            @PathVariable Long productId,
            @RequestParam
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity
    ) {
        InventoryResponseDTO response =
                inventoryService.updateInventory(productId, quantity);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}/available")
    public ResponseEntity<Boolean> isStockAvailable(
            @PathVariable Long productId,
            @RequestParam
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity
    ) {
        boolean available =
                inventoryService.isStockAvailable(productId, quantity);

        return ResponseEntity.ok(available);
    }
}