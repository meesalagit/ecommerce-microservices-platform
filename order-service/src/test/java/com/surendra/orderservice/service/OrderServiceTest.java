package com.surendra.orderservice.service;

import com.surendra.orderservice.dto.OrderRequestDTO;
import com.surendra.orderservice.dto.OrderResponseDTO;
import com.surendra.orderservice.entity.Order;
import com.surendra.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String USER_SERVICE = "http://user-service";
    private static final String PRODUCT_SERVICE = "http://product-service";
    private static final String INVENTORY_SERVICE = "http://inventory-service";
    private static final String INTERNAL_KEY = "test-internal-key";
    private static final String AUTH_HEADER = "Bearer test-jwt";

    @Mock
    private OrderRepository orderRepository;

    // Fake user/product/inventory services: answer RestClient calls in order
    private MockRestServiceServer server;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        orderService = new OrderService(orderRepository, builder.build());

        // @Value fields are normally filled by Spring; set them by hand here
        ReflectionTestUtils.setField(orderService, "userServiceUrl", USER_SERVICE);
        ReflectionTestUtils.setField(orderService, "productServiceUrl", PRODUCT_SERVICE);
        ReflectionTestUtils.setField(orderService, "inventoryServiceUrl", INVENTORY_SERVICE);
        ReflectionTestUtils.setField(orderService, "internalServiceKey", INTERNAL_KEY);
    }

    // ---------- helpers ----------

    private OrderRequestDTO request(Long productId, int quantity) {
        OrderRequestDTO dto = new OrderRequestDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        return dto;
    }

    private void expectUserLookup() {
        server.expect(requestTo(USER_SERVICE + "/users/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", AUTH_HEADER))
                .andRespond(withSuccess(
                        "{\"id\":7,\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane@test.com\"}",
                        MediaType.APPLICATION_JSON));
    }

    private void expectProductLookup() {
        server.expect(requestTo(PRODUCT_SERVICE + "/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":1,\"name\":\"Laptop\",\"price\":999.99,\"category\":\"Electronics\"}",
                        MediaType.APPLICATION_JSON));
    }

    private void expectStockCheck(boolean available) {
        server.expect(requestTo(INVENTORY_SERVICE + "/inventory/1/available?quantity=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Key", INTERNAL_KEY))
                .andRespond(withSuccess(String.valueOf(available), MediaType.APPLICATION_JSON));
    }

    private void expectStockReduce() {
        server.expect(requestTo(INVENTORY_SERVICE + "/inventory/1/reduce?quantity=2"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Internal-Service-Key", INTERNAL_KEY))
                .andRespond(withSuccess());
    }

    // ---------- tests ----------

    @Test
    void createOrder_happyPath_reducesStockAndSavesOrderWithCorrectTotal() {
        expectUserLookup();
        expectProductLookup();
        expectStockCheck(true);
        expectStockReduce();

        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        OrderResponseDTO response = orderService.createOrder(AUTH_HEADER, request(1L, 2));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getProductPrice()).isEqualByComparingTo("999.99");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1999.98");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo("CREATED");
        assertThat(response.getCreatedAt()).isNotNull();

        server.verify(); // all 4 downstream calls happened, in order
    }

    @Test
    void createOrder_notEnoughStock_throwsAndNeverReducesOrSaves() {
        expectUserLookup();
        expectProductLookup();
        expectStockCheck(false);
        // no reduce expected: an unexpected call would fail the test

        assertThatThrownBy(() -> orderService.createOrder(AUTH_HEADER, request(1L, 2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderRepository, never()).saveAndFlush(any());
        server.verify();
    }

    @Test
    void createOrder_saveFails_restoresInventory() {
        expectUserLookup();
        expectProductLookup();
        expectStockCheck(true);
        expectStockReduce();

        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenThrow(new RuntimeException("Database down"));

        // Compensation: the stock taken above must be put back
        server.expect(requestTo(INVENTORY_SERVICE + "/inventory/1/restore?quantity=2"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Internal-Service-Key", INTERNAL_KEY))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> orderService.createOrder(AUTH_HEADER, request(1L, 2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory was restored");

        server.verify(); // proves the restore call was made
    }
}
