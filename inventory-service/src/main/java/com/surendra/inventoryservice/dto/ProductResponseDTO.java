package com.surendra.inventoryservice.dto;

import java.math.BigDecimal;

public class ProductResponseDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }
}