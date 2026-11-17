package com.surendra.inventoryservice.dto;

public class InventoryResponseDTO {

    private Long id;
    private Long productId;
    private Integer quantity;

    public InventoryResponseDTO(
            Long id,
            Long productId,
            Integer quantity
    ) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }
}