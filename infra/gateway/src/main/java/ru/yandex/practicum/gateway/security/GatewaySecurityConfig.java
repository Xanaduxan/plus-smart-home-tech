package ru.yandex.practicum.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) {
        String[] administrativePaths = {
                "/api/products/**",
                "/api/categories/**",
                "/api/inventory/**"
        };

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource
                ))
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .securityContextRepository(
                        NoOpServerSecurityContextRepository.getInstance()
                )
                .requestCache(cache -> cache.requestCache(
                        NoOpServerRequestCache.getInstance()
                ))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.GET,
                                "/docs/products/v3/api-docs/**",
                                "/docs/products/v3/api-docs.yaml",
                                "/docs/products/swagger-ui.html",
                                "/docs/products/swagger-ui/**",

                                "/docs/inventory/v3/api-docs/**",
                                "/docs/inventory/v3/api-docs.yaml",
                                "/docs/inventory/swagger-ui.html",
                                "/docs/inventory/swagger-ui/**",

                                "/docs/orders/v3/api-docs/**",
                                "/docs/orders/v3/api-docs.yaml",
                                "/docs/orders/swagger-ui.html",
                                "/docs/orders/swagger-ui/**"
                        )
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/inventory/**"
                        )
                        .permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/orders")
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders/by-email",
                                "/api/orders/{id}"
                        )
                        .hasRole("USER")

                        .pathMatchers(HttpMethod.POST, "/api/orders/**")
                        .hasRole("USER")

                        .pathMatchers(HttpMethod.POST, administrativePaths)
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.PUT, administrativePaths)
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.PATCH, administrativePaths)
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.DELETE, administrativePaths)
                        .hasRole("ADMIN")

                        .anyExchange().denyAll()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(
            GatewaySecurityProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        List<UserDetails> users = properties.users().stream()
                .map(user -> User.withUsername(user.username())
                        .password(passwordEncoder.encode(user.password()))
                        .roles(user.roles().toArray(String[]::new))
                        .build())
                .toList();

        return new MapReactiveUserDetailsService(users);
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            GlobalCorsProperties properties
    ) {
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.setCorsConfigurations(properties.getCorsConfigurations());
        return source;
    }
}