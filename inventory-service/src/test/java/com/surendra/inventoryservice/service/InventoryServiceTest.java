package com.surendra.inventoryservice.service;

import com.surendra.inventoryservice.dto.InventoryRequestDTO;
import com.surendra.inventoryservice.dto.InventoryResponseDTO;
import com.surendra.inventoryservice.entity.Inventory;
import com.surendra.inventoryservice.exception.InsufficientStockException;
import com.surendra.inventoryservice.exception.InventoryAlreadyExistsException;
import com.surendra.inventoryservice.exception.InventoryNotFoundException;
import com.surendra.inventoryservice.exception.ProductNotFoundException;
import com.surendra.inventoryservice.exception.ProductServiceUnavailableException;
import com.surendra.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final String PRODUCT_SERVICE = "http://product-service";

    @Mock
    private InventoryRepository inventoryRepository;

    // Fake product-service: answers the RestClient calls without a real server
    private MockRestServiceServer productServer;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PRODUCT_SERVICE);
        productServer = MockRestServiceServer.bindTo(builder).build();
        inventoryService = new InventoryService(inventoryRepository, builder.build());
    }

    // ---------- helpers ----------

    private InventoryRequestDTO request(Long productId, int quantity) {
        InventoryRequestDTO dto = new InventoryRequestDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        return dto;
    }

    private Inventory inventory(Long productId, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setId(10L);
        inventory.setProductId(productId);
        inventory.setQuantity(quantity);
        return inventory;
    }

    private void productExists(Long productId) {
        productServer.expect(requestTo(PRODUCT_SERVICE + "/products/" + productId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":" + productId + ",\"name\":\"Laptop\",\"price\":999.99}",
                        MediaType.APPLICATION_JSON));
    }

    // ---------- createInventory (calls product-service) ----------

    @Test
    void createInventory_productExists_savesStock() {
        productExists(1L);
        when(inventoryRepository.existsByProductId(1L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        InventoryResponseDTO response = inventoryService.createInventory(request(1L, 10));

        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getQuantity()).isEqualTo(10);
        productServer.verify();
    }

    @Test
    void createInventory_productNotFound_throwsProductNotFound() {
        productServer.expect(requestTo(PRODUCT_SERVICE + "/products/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> inventoryService.createInventory(request(99L, 5)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void createInventory_productServiceDown_throwsServiceUnavailable() {
        productServer.expect(requestTo(PRODUCT_SERVICE + "/products/1"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> inventoryService.createInventory(request(1L, 5)))
                .isInstanceOf(ProductServiceUnavailableException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void createInventory_alreadyExists_throws() {
        productExists(1L);
        when(inventoryRepository.existsByProductId(1L)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createInventory(request(1L, 5)))
                .isInstanceOf(InventoryAlreadyExistsException.class);

        verify(inventoryRepository, never()).save(any());
    }

    // ---------- read / update ----------

    @Test
    void getInventoryByProductId_notFound_throws() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryByProductId(99L))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    void updateInventory_addsToExistingQuantity() {
        Inventory existing = inventory(1L, 10);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(existing)).thenReturn(existing);

        InventoryResponseDTO response = inventoryService.updateInventory(1L, 5);

        assertThat(response.getQuantity()).isEqualTo(15);
    }

    // ---------- reduceStock (used by order-service) ----------

    @Test
    void reduceStock_enoughStock_subtractsQuantity() {
        Inventory existing = inventory(1L, 10);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(existing)).thenReturn(existing);

        InventoryResponseDTO response = inventoryService.reduceStock(1L, 2);

        assertThat(response.getQuantity()).isEqualTo(8);
    }

    @Test
    void reduceStock_notEnoughStock_throwsAndDoesNotSave() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(1L, 1)));

        assertThatThrownBy(() -> inventoryService.reduceStock(1L, 5))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reduceStock_exactQuantity_leavesZero() {
        Inventory existing = inventory(1L, 3);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(existing)).thenReturn(existing);

        assertThat(inventoryService.reduceStock(1L, 3).getQuantity()).isZero();
    }

    // ---------- isStockAvailable ----------

    @Test
    void isStockAvailable_trueWhenEnough_falseWhenNot() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(1L, 5)));

        assertThat(inventoryService.isStockAvailable(1L, 5)).isTrue();
        assertThat(inventoryService.isStockAvailable(1L, 6)).isFalse();
    }
}
