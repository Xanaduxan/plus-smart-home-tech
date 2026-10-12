package ru.yandex.practicum.analyzer.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean(destroyMethod = "")
    public Consumer<String, HubEventAvro> hubEventConsumer(
            AnalyzerKafkaProperties properties
    ) {
        return new KafkaConsumer<>(
                consumerConfig(
                        properties.bootstrapServers(),
                        properties.hubs()
                )
        );
    }

    @Bean(destroyMethod = "")
    public Consumer<String, SensorsSnapshotAvro> snapshotConsumer(
            AnalyzerKafkaProperties properties
    ) {
        return new KafkaConsumer<>(
                consumerConfig(
                        properties.bootstrapServers(),
                        properties.snapshots()
                )
        );
    }

    private Map<String, Object> consumerConfig(
            String bootstrapServers,
            AnalyzerKafkaProperties.ConsumerProperties consumer
    ) {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumer.groupId());
        config.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                consumer.enableAutoCommit()
        );
        config.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                consumer.autoOffsetReset()
        );
        config.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                consumer.maxPollRecords()
        );
        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                consumer.keyDeserializer()
        );
        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                consumer.valueDeserializer()
        );

        return config;
    }
}