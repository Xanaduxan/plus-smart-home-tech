package ru.yandex.practicum.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import ru.yandex.practicum.gateway.Gateway;

import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(
        classes = Gateway.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.config.name=gateway-security-test"
)
@AutoConfigureWebTestClient
@Import(GatewaySecurityConfigTest.TestBackendConfig.class)
class GatewaySecurityConfigTest {

    private static final String BACKEND_HEADER = "X-Test-Backend";
    private static final String ORIGIN = "http://localhost:8443";

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void configureWebTestClient() {
        webTestClient = webTestClient.mutate()
                .baseUrl("http://localhost:8080")
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/products",
            "/api/products/1",
            "/api/categories",
            "/api/categories/1",
            "/api/inventory",
            "/api/inventory/1"
    })
    void catalogGet_withoutCredentials_isPublic(String path) {
        expectBackend(exchange(HttpMethod.GET, path, null));
    }

    @Test
    void orderCreate_withoutCredentials_isUnauthorized() {
        exchange(HttpMethod.POST, "/api/orders", null)
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ivan", "anna"})
    void orderCreate_withUserRole_passesSecurity(String username) {
        expectBackend(exchange(HttpMethod.POST, "/api/orders", username));
    }

    @ParameterizedTest
    @MethodSource("administrativeWrites")
    void catalogWrite_withIvan_isForbidden(
            HttpMethod method,
            String path
    ) {
        exchange(method, path, "ivan")
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    @ParameterizedTest
    @MethodSource("administrativeWrites")
    void catalogWrite_withAnna_passesSecurity(
            HttpMethod method,
            String path
    ) {
        expectBackend(exchange(method, path, "anna"));
    }

    @Test
    void orderList_withIvan_isForbidden() {
        exchange(HttpMethod.GET, "/api/orders", "ivan")
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    @Test
    void orderList_withAnna_passesSecurity() {
        expectBackend(exchange(HttpMethod.GET, "/api/orders", "anna"));
    }

    @ParameterizedTest
    @CsvSource({
            "ivan, /api/orders/1",
            "anna, /api/orders/1",
            "ivan, /api/orders/by-email?email=ivan@example.com",
            "anna, /api/orders/by-email?email=anna@example.com"
    })
    void orderRead_withUserRole_passesSecurity(
            String username,
            String path
    ) {
        expectBackend(exchange(HttpMethod.GET, path, username));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/orders/1",
            "/api/orders/by-email?email=ivan@example.com"
    })
    void orderRead_withoutCredentials_isUnauthorized(String path) {
        exchange(HttpMethod.GET, path, null)
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/unknown",
            "/api/orders/1/items",
            "/docs/orders/unknown"
    })
    void unknownRoute_withAnna_isForbidden(String path) {
        exchange(HttpMethod.GET, path, "anna")
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    @Test
    void unlistedOrderWrite_withAnna_isForbidden() {
        exchange(HttpMethod.DELETE, "/api/orders/1", "anna")
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    @ParameterizedTest
    @MethodSource("documentationPaths")
    void documentationGet_withoutCredentials_isPublic(String path) {
        expectBackend(exchange(HttpMethod.GET, path, null));
    }

    @Test
    void options_withoutCredentials_passesSecurity() {
        expectBackend(exchange(HttpMethod.OPTIONS, "/api/orders", null));
    }

    @Test
    void corsPreflight_withoutCredentials_isPublic() {
        webTestClient.options()
                .uri("/api/orders")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                        "POST"
                )
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                        "authorization,content-type"
                )
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        ORIGIN
                )
                .expectHeader().valueMatches(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        ".*POST.*"
                )
                .expectHeader().valueMatches(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        "(?i).*authorization.*"
                )
                .expectHeader().valueMatches(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        "(?i).*content-type.*"
                )
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    @Test
    void wrongPassword_isUnauthorized() {
        webTestClient.post()
                .uri("/api/orders")
                .headers(headers ->
                        headers.setBasicAuth("ivan", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    @Test
    void authentication_isNotRetainedBetweenRequests() {
        WebTestClient.ResponseSpec authenticated =
                exchange(HttpMethod.POST, "/api/orders", "ivan");

        authenticated.expectHeader()
                .doesNotExist(HttpHeaders.SET_COOKIE);
        expectBackend(authenticated);

        exchange(HttpMethod.POST, "/api/orders", null)
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist(BACKEND_HEADER);
    }

    private WebTestClient.ResponseSpec exchange(
            HttpMethod method,
            String path,
            String username
    ) {
        WebTestClient.RequestBodySpec request =
                webTestClient.method(method).uri(path);

        if (username != null) {
            request.headers(headers ->
                    headers.setBasicAuth(username, username));
        }

        return request.exchange();
    }

    private void expectBackend(WebTestClient.ResponseSpec response) {
        response.expectStatus().isOk()
                .expectHeader().valueEquals(BACKEND_HEADER, "reached")
                .expectBody(String.class).isEqualTo("test-backend");
    }

    static Stream<Arguments> administrativeWrites() {
        return Stream.of(
                HttpMethod.POST,
                HttpMethod.PUT,
                HttpMethod.PATCH,
                HttpMethod.DELETE
        ).flatMap(method -> Stream.of(
                "/api/products/1",
                "/api/categories/1",
                "/api/inventory/1"
        ).map(path -> Arguments.of(method, path)));
    }

    static Stream<String> documentationPaths() {
        return Stream.of("products", "inventory", "orders")
                .flatMap(service -> Stream.of(
                        "/v3/api-docs",
                        "/v3/api-docs/swagger-config",
                        "/v3/api-docs.yaml",
                        "/swagger-ui.html",
                        "/swagger-ui/index.html"
                ).map(suffix -> "/docs/" + service + suffix));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBackendConfig {

        @Bean
        RouteLocator testGatewayRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("test-backend-route", route -> route
                            .path("/**")
                            .uri("forward:/test-backend"))
                    .build();
        }

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route(
                    path("/test-backend"),
                    request -> ServerResponse.ok()
                            .header(BACKEND_HEADER, "reached")
                            .bodyValue("test-backend")
            );
        }
    }
}