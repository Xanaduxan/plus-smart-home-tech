package ru.yandex.practicum.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import ru.yandex.practicum.gateway.security.GatewaySecurityProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class Gateway {

    public static void main(String[] args) {
        SpringApplication.run(Gateway.class, args);
    }
}