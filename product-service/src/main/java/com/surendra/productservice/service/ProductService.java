package com.surendra.productservice.service;

import com.surendra.productservice.dto.ProductRequestDTO;
import com.surendra.productservice.dto.ProductResponseDTO;
import com.surendra.productservice.entity.Product;
import com.surendra.productservice.exception.ProductNotFoundException;
import com.surendra.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import com.surendra.productservice.exception.ProductAlreadyExistsException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        if (productRepository.existsByNameIgnoreCase(requestDTO.getName())) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with name: " + requestDTO.getName()
            );
        }

        Product product = new Product();

        product.setName(requestDTO.getName());
        product.setDescription(requestDTO.getDescription());
        product.setPrice(requestDTO.getPrice());
        product.setCategory(requestDTO.getCategory());

        Product savedProduct = productRepository.save(product);

        return new ProductResponseDTO(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getDescription(),
                savedProduct.getPrice(),
                savedProduct.getCategory()
        );
    }

    public List<ProductResponseDTO> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(product -> new ProductResponseDTO(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getCategory()
                ))
                .toList();
    }

    public ProductResponseDTO getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + id
                ));

        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory()
        );
    }

    public ProductResponseDTO updateProduct(
            Long id,
            ProductRequestDTO requestDTO
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + id
                ));
        if (productRepository.existsByNameIgnoreCaseAndIdNot(
                requestDTO.getName(),
                id
        )) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with name: " + requestDTO.getName()
            );
        }

        product.setName(requestDTO.getName());
        product.setDescription(requestDTO.getDescription());
        product.setPrice(requestDTO.getPrice());
        product.setCategory(requestDTO.getCategory());

        Product updatedProduct = productRepository.save(product);

        return new ProductResponseDTO(
                updatedProduct.getId(),
                updatedProduct.getName(),
                updatedProduct.getDescription(),
                updatedProduct.getPrice(),
                updatedProduct.getCategory()
        );
    }

    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + id
                ));

        productRepository.delete(product);
    }

    public List<ProductResponseDTO> searchProductsByName(String name) {

        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(product -> new ProductResponseDTO(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getCategory()
                ))
                .toList();
    }

    public List<ProductResponseDTO> searchProductsByCategory(String category) {

        return productRepository.findByCategoryIgnoreCase(category)
                .stream()
                .map(product -> new ProductResponseDTO(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getCategory()
                ))
                .toList();
    }
}