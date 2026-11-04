package ru.yandex.practicum.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record GatewaySecurityProperties(List<UserProperties> users) {

    public record UserProperties(
            String username,
            String password,
            List<String> roles
    ) {
    }
}