package ru.yandex.practicum.aggregator.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.kafka")
public class AggregatorKafkaProperties {

    private String bootstrapServers;
    private ConsumerProperties consumer;
    private ProducerProperties producer;

    @Getter
    @Setter
    public static class ConsumerProperties {

        private String groupId;
        private boolean enableAutoCommit;
        private String autoOffsetReset;
        private int maxPollRecords;
        private String keyDeserializer;
        private String valueDeserializer;
    }

    @Getter
    @Setter
    public static class ProducerProperties {

        private String acks;
        private String keySerializer;
        private String valueSerializer;
    }
}
