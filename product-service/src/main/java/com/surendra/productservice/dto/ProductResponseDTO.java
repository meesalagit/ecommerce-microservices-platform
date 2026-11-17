package com.surendra.productservice.dto;

import java.math.BigDecimal;

public class ProductResponseDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;

    public ProductResponseDTO(
            Long id,
            String name,
            String description,
            BigDecimal price,
            String category
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

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