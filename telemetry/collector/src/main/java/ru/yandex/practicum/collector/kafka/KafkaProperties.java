package ru.yandex.practicum.collector.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "collector.kafka")
public class KafkaProperties {

    private String bootstrapServers;
    private Topics topics;

    @Getter
    @Setter
    public static class Topics {

        private String sensors;
        private String hubs;
    }
}