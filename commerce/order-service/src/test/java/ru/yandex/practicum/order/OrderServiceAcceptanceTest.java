package ru.yandex.practicum.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.client.dto.ProductResponse;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderItemRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unchecked")
class OrderServiceAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean
    private ProductClient productClient;

    @Test
    void shouldCreateOrderStoreProductSnapshotAndFindOrderByIdAndEmail()
            throws Exception {

        when(productClient.getProductById(1L))
                .thenReturn(new ProductResponse(
                        1L,
                        "Acceptance Smart Lamp",
                        new BigDecimal("3490.00")
                ));

        when(productClient.getProductById(2L))
                .thenReturn(new ProductResponse(
                        2L,
                        "Acceptance Smart Plug",
                        new BigDecimal("1290.00")
                ));

        CreateOrderRequest request = new CreateOrderRequest(
                "Acceptance Buyer",
                "acceptance-buyer@example.com",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                )
        );

        MvcResult createResponse = postJson("/api/orders", request);

        assertThat(status(createResponse))
                .as("Создание заказа должно возвращать HTTP 201")
                .isEqualTo(201);

        Map<String, Object> created = readMap(createResponse);
        Long orderId = asLong(created.get("id"));

        assertThat(orderId)
                .as("Созданный заказ должен содержать id")
                .isNotNull();

        assertThat(created.get("status"))
                .as("Новый заказ должен иметь статус CREATED")
                .isEqualTo("CREATED");

        assertThat(asDecimal(created.get("totalPrice")))
                .as("Сумма заказа рассчитывается по ценам из каталога")
                .isEqualByComparingTo("8270.00");

        assertProductSnapshots(
                (List<Map<String, Object>>) created.get("items")
        );

        MvcResult byIdResponse = mvc.perform(
                get("/api/orders/{id}", orderId)
        ).andReturn();

        assertThat(status(byIdResponse))
                .as("Получение созданного заказа должно возвращать HTTP 200")
                .isEqualTo(200);

        Map<String, Object> savedOrder = readMap(byIdResponse);

        assertThat(asLong(savedOrder.get("id")))
                .isEqualTo(orderId);

        assertThat(savedOrder.get("customerEmail"))
                .isEqualTo("acceptance-buyer@example.com");

        assertThat(asDecimal(savedOrder.get("totalPrice")))
                .as("Сумма должна сохраняться после чтения из БД")
                .isEqualByComparingTo("8270.00");

        assertProductSnapshots(
                (List<Map<String, Object>>) savedOrder.get("items")
        );

        MvcResult byEmailResponse = mvc.perform(
                get("/api/orders/by-email")
                        .param("email", "acceptance-buyer@example.com")
        ).andReturn();

        assertThat(status(byEmailResponse))
                .as("Поиск заказов по email должен возвращать HTTP 200")
                .isEqualTo(200);

        assertThat(readList(byEmailResponse))
                .as("Поиск должен вернуть созданный заказ")
                .anySatisfy(item -> {
                    assertThat(asLong(item.get("id")))
                            .isEqualTo(orderId);
                    assertThat(item.get("customerEmail"))
                            .isEqualTo("acceptance-buyer@example.com");
                });
    }

    @Test
    void shouldReturnBadRequestForInvalidOrderPayload() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                "",
                "not-an-email",
                List.of()
        );

        MvcResult response = postJson("/api/orders", invalidRequest);

        assertThat(status(response))
                .as("Невалидный запрос должен возвращать HTTP 400")
                .isEqualTo(400);

        assertThat(readMap(response))
                .as("Ответ должен содержать детали ошибки валидации")
                .containsKeys("message", "validationErrors");

        verifyNoInteractions(productClient);
    }

    private static void assertProductSnapshots(
            List<Map<String, Object>> items
    ) {
        assertThat(items)
                .as("Заказ должен содержать снимки двух товаров")
                .hasSize(2)
                .anySatisfy(item -> {
                    assertThat(asLong(item.get("productId")))
                            .isEqualTo(1L);
                    assertThat(item.get("productName"))
                            .isEqualTo("Acceptance Smart Lamp");
                    assertThat(asDecimal(item.get("price")))
                            .isEqualByComparingTo("3490.00");
                    assertThat(asLong(item.get("quantity")))
                            .isEqualTo(2L);
                })
                .anySatisfy(item -> {
                    assertThat(asLong(item.get("productId")))
                            .isEqualTo(2L);
                    assertThat(item.get("productName"))
                            .isEqualTo("Acceptance Smart Plug");
                    assertThat(asDecimal(item.get("price")))
                            .isEqualByComparingTo("1290.00");
                    assertThat(asLong(item.get("quantity")))
                            .isEqualTo(1L);
                });
    }

    private MvcResult postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andReturn();
    }

    private static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private Map<String, Object> readMap(MvcResult result) throws Exception {
        return json.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
    }

    private List<Map<String, Object>> readList(MvcResult result)
            throws Exception {
        return json.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static BigDecimal asDecimal(Object value) {
        return new BigDecimal(value.toString());
    }
}