package ru.yandex.practicum.aggregator.kafka;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean(destroyMethod = "")
    public Consumer<String, SensorEventAvro> kafkaConsumer(
            AggregatorKafkaProperties properties
    ) {
        AggregatorKafkaProperties.ConsumerProperties consumer =
                properties.getConsumer();

        Map<String, Object> config = new HashMap<>();

        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getBootstrapServers()
        );
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumer.getGroupId());
        config.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                consumer.isEnableAutoCommit()
        );
        config.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                consumer.getAutoOffsetReset()
        );
        config.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                consumer.getMaxPollRecords()
        );
        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                consumer.getKeyDeserializer()
        );
        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                consumer.getValueDeserializer()
        );

        return new KafkaConsumer<>(config);
    }

    @Bean(destroyMethod = "")
    public Producer<String, SpecificRecordBase> kafkaProducer(
            AggregatorKafkaProperties properties
    ) {
        AggregatorKafkaProperties.ProducerProperties producer =
                properties.getProducer();

        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getBootstrapServers()
        );
        config.put(ProducerConfig.ACKS_CONFIG, producer.getAcks());
        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                producer.getKeySerializer()
        );
        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                producer.getValueSerializer()
        );

        return new KafkaProducer<>(config);
    }
}
