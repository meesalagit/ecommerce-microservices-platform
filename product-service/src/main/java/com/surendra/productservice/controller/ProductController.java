package com.surendra.productservice.controller;

import com.surendra.productservice.dto.ProductRequestDTO;
import com.surendra.productservice.dto.ProductResponseDTO;
import com.surendra.productservice.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO requestDTO
    ) {
        ProductResponseDTO response = productService.createProduct(requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {

        List<ProductResponseDTO> products = productService.getAllProducts();

        return ResponseEntity.ok(products);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(
            @PathVariable Long id
    ) {
        ProductResponseDTO product = productService.getProductById(id);

        return ResponseEntity.ok(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO requestDTO
    ) {
        ProductResponseDTO updatedProduct =
                productService.updateProduct(id, requestDTO);

        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id
    ) {
        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> searchProductsByName(
            @RequestParam String name
    ) {
        List<ProductResponseDTO> products =
                productService.searchProductsByName(name);

        return ResponseEntity.ok(products);
    }

    @GetMapping("/category")
    public ResponseEntity<List<ProductResponseDTO>> searchProductsByCategory(
            @RequestParam String name
    ) {
        List<ProductResponseDTO> products =
                productService.searchProductsByCategory(name);

        return ResponseEntity.ok(products);
    }
}