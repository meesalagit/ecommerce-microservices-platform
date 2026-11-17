package com.surendra.productservice.service;

import com.surendra.productservice.dto.ProductRequestDTO;
import com.surendra.productservice.dto.ProductResponseDTO;
import com.surendra.productservice.entity.Product;
import com.surendra.productservice.exception.ProductAlreadyExistsException;
import com.surendra.productservice.exception.ProductNotFoundException;
import com.surendra.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    // ---------- helpers ----------

    private ProductRequestDTO request(String name, String price, String category) {
        ProductRequestDTO dto = new ProductRequestDTO();
        ReflectionTestUtils.setField(dto, "name", name);
        ReflectionTestUtils.setField(dto, "description", name + " description");
        ReflectionTestUtils.setField(dto, "price", new BigDecimal(price));
        ReflectionTestUtils.setField(dto, "category", category);
        return dto;
    }

    private Product product(Long id, String name, String price, String category) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(new BigDecimal(price));
        product.setCategory(category);
        return product;
    }

    // ---------- createProduct ----------

    @Test
    void createProduct_newName_savesAndReturnsDto() {
        when(productRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductResponseDTO response =
                productService.createProduct(request("Laptop", "999.99", "Electronics"));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Laptop");
        assertThat(captor.getValue().getCategory()).isEqualTo("Electronics");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPrice()).isEqualByComparingTo("999.99");
    }

    @Test
    void createProduct_duplicateName_throwsAndDoesNotSave() {
        when(productRepository.existsByNameIgnoreCase("Laptop")).thenReturn(true);

        assertThatThrownBy(() ->
                productService.createProduct(request("Laptop", "999.99", "Electronics")))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessageContaining("Laptop");

        verify(productRepository, never()).save(any());
    }

    // ---------- read ----------

    @Test
    void getAllProducts_mapsEveryProduct() {
        when(productRepository.findAll()).thenReturn(List.of(
                product(1L, "Laptop", "999.99", "Electronics"),
                product(2L, "Desk", "150.00", "Furniture")
        ));

        List<ProductResponseDTO> products = productService.getAllProducts();

        assertThat(products).extracting(ProductResponseDTO::getName)
                .containsExactly("Laptop", "Desk");
    }

    @Test
    void getProductById_found_returnsDto() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product(1L, "Laptop", "999.99", "Electronics")));

        ProductResponseDTO response = productService.getProductById(1L);

        assertThat(response.getName()).isEqualTo("Laptop");
    }

    @Test
    void getProductById_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---------- updateProduct ----------

    @Test
    void updateProduct_valid_updatesAllFields() {
        Product existing = product(1L, "Laptop", "999.99", "Electronics");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Laptop Pro", 1L)).thenReturn(false);
        when(productRepository.save(existing)).thenReturn(existing);

        ProductResponseDTO response =
                productService.updateProduct(1L, request("Laptop Pro", "1299.00", "Computers"));

        assertThat(response.getName()).isEqualTo("Laptop Pro");
        assertThat(response.getPrice()).isEqualByComparingTo("1299.00");
        assertThat(response.getCategory()).isEqualTo("Computers");
    }

    @Test
    void updateProduct_nameUsedByAnotherProduct_throws() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product(1L, "Laptop", "999.99", "Electronics")));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Desk", 1L)).thenReturn(true);

        assertThatThrownBy(() ->
                productService.updateProduct(1L, request("Desk", "150.00", "Furniture")))
                .isInstanceOf(ProductAlreadyExistsException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.updateProduct(99L, request("Laptop", "999.99", "Electronics")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ---------- deleteProduct ----------

    @Test
    void deleteProduct_found_deletes() {
        Product existing = product(1L, "Laptop", "999.99", "Electronics");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(1L);

        verify(productRepository).delete(existing);
    }

    @Test
    void deleteProduct_notFound_throwsAndDoesNotDelete() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).delete(any());
    }

    // ---------- search ----------

    @Test
    void searchProductsByName_returnsMatches() {
        when(productRepository.findByNameContainingIgnoreCase("lap"))
                .thenReturn(List.of(product(1L, "Laptop", "999.99", "Electronics")));

        assertThat(productService.searchProductsByName("lap"))
                .extracting(ProductResponseDTO::getName)
                .containsExactly("Laptop");
    }

    @Test
    void searchProductsByCategory_noMatches_returnsEmptyList() {
        when(productRepository.findByCategoryIgnoreCase("Toys")).thenReturn(List.of());

        assertThat(productService.searchProductsByCategory("Toys")).isEmpty();
    }
}
