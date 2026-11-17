package com.surendra.inventoryservice.service;

import com.surendra.inventoryservice.dto.InventoryRequestDTO;
import com.surendra.inventoryservice.dto.InventoryResponseDTO;
import com.surendra.inventoryservice.entity.Inventory;
import com.surendra.inventoryservice.exception.InventoryAlreadyExistsException;
import com.surendra.inventoryservice.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import com.surendra.inventoryservice.exception.InventoryNotFoundException;
import com.surendra.inventoryservice.exception.InsufficientStockException;
import org.springframework.web.client.RestClient;
import com.surendra.inventoryservice.dto.ProductResponseDTO;
import com.surendra.inventoryservice.exception.ProductNotFoundException;
import org.springframework.web.client.HttpClientErrorException;
import com.surendra.inventoryservice.exception.ProductServiceUnavailableException;
import org.springframework.web.client.ResourceAccessException;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RestClient restClient;
    public InventoryService(
            InventoryRepository inventoryRepository,
            RestClient restClient
    ) {
        this.inventoryRepository = inventoryRepository;
        this.restClient = restClient;
    }

    public InventoryResponseDTO createInventory(
            InventoryRequestDTO requestDTO
    ) {

        try {
            restClient.get()
                    .uri("/products/{id}", requestDTO.getProductId())
                    .retrieve()
                    .body(ProductResponseDTO.class);

        } catch (HttpClientErrorException.NotFound exception) {
            throw new ProductNotFoundException(
                    "Product not found with id: " + requestDTO.getProductId()
            );
        }
        catch (ResourceAccessException exception) {
            throw new ProductServiceUnavailableException(
                    "Product Service is currently unavailable"
            );
        }

        if (inventoryRepository.existsByProductId(requestDTO.getProductId())) {
            throw new InventoryAlreadyExistsException(
                    "Inventory already exists for product ID: "
                            + requestDTO.getProductId()
            );
        }

        Inventory inventory = new Inventory();

        inventory.setProductId(requestDTO.getProductId());
        inventory.setQuantity(requestDTO.getQuantity());

        Inventory savedInventory = inventoryRepository.save(inventory);

        return new InventoryResponseDTO(
                savedInventory.getId(),
                savedInventory.getProductId(),
                savedInventory.getQuantity()
        );
    }

    public InventoryResponseDTO getInventoryByProductId(Long productId) {

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        return new InventoryResponseDTO(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity()
        );
    }

    public InventoryResponseDTO updateInventory(
            Long productId,
            Integer quantity
    ) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        inventory.setQuantity(
                inventory.getQuantity() + quantity
        );

        Inventory updatedInventory = inventoryRepository.save(inventory);

        return new InventoryResponseDTO(
                updatedInventory.getId(),
                updatedInventory.getProductId(),
                updatedInventory.getQuantity()
        );
    }

    public InventoryResponseDTO reduceStock(
            Long productId,
            Integer quantity
    ) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        if (inventory.getQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product ID: " + productId
            );
        }

        inventory.setQuantity(
                inventory.getQuantity() - quantity
        );

        Inventory updatedInventory = inventoryRepository.save(inventory);

        return new InventoryResponseDTO(
                updatedInventory.getId(),
                updatedInventory.getProductId(),
                updatedInventory.getQuantity()
        );
    }

    public boolean isStockAvailable(
            Long productId,
            Integer quantity
    ) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for product ID: " + productId
                ));

        return inventory.getQuantity() >= quantity;
    }
}