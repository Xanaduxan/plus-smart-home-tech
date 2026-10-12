package ru.yandex.practicum.analyzer.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "analyzer.kafka")
public record AnalyzerKafkaProperties(
        String bootstrapServers,
        Duration pollTimeout,
        ConsumerProperties hubs,
        ConsumerProperties snapshots
) {

    public record ConsumerProperties(
            String topic,
            String groupId,
            boolean enableAutoCommit,
            String autoOffsetReset,
            int maxPollRecords,
            String keyDeserializer,
            String valueDeserializer
    ) {
    }
}