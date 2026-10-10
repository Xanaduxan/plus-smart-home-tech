package ru.yandex.practicum.collector.kafka;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class KafkaConsumerApplication {
    private static final Logger log =
            LoggerFactory.getLogger(KafkaConsumerApplication.class);

    private static final String TOPIC = "telemetry.sensors.v1";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);
    private static final int COMMIT_EVERY = 10;

    public static void main(String[] args) throws IOException {
        KafkaConsumer<String, byte[]> consumer =
                new KafkaConsumer<>(getConsumerProperties());
        Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
        SpecificDatumReader<SensorEventAvro> reader =
                new SpecificDatumReader<>(SensorEventAvro.getClassSchema());
        CountDownLatch closed = new CountDownLatch(1);

        Thread shutdownHook = new Thread(() -> {
            consumer.wakeup();
            try {
                closed.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "sensor-consumer-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            consumer.subscribe(List.of(TOPIC), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    try {
                        if (!currentOffsets.isEmpty()) {
                            consumer.commitSync(new HashMap<>(currentOffsets));
                        }
                    } finally {
                        partitions.forEach(currentOffsets::remove);
                    }
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    log.info("Назначены партиции: {}", partitions);
                }

                @Override
                public void onPartitionsLost(Collection<TopicPartition> partitions) {
                    partitions.forEach(currentOffsets::remove);
                }
            });

            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(POLL_TIMEOUT);
                int count = 0;

                for (ConsumerRecord<String, byte[]> record : records) {
                    handleRecord(record, reader);

                    // Смещение обновляем только после успешной обработки.
                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );

                    count++;
                    if (count % COMMIT_EVERY == 0) {
                        commitAsync(consumer, currentOffsets);
                    }
                }

                // Фиксируем оставшиеся обработанные записи из этой выборки.
                if (count % COMMIT_EVERY != 0) {
                    commitAsync(consumer, currentOffsets);
                }
            }
        } catch (WakeupException ignored) {
            log.info("Останавливаем потребителя");
        } finally {
            try {
                if (!currentOffsets.isEmpty()) {
                    consumer.commitSync(new HashMap<>(currentOffsets));
                }
            } catch (KafkaException exception) {
                log.error("Не удалось зафиксировать смещения при закрытии", exception);
            } finally {
                try {
                    consumer.close();
                } finally {
                    closed.countDown();
                    try {
                        Runtime.getRuntime().removeShutdownHook(shutdownHook);
                    } catch (IllegalStateException ignored) {
                        // JVM уже выполняет завершение работы.
                    }
                }
            }
        }
    }

    private static void handleRecord(
            ConsumerRecord<String, byte[]> record,
            SpecificDatumReader<SensorEventAvro> reader
    ) throws IOException {
        if (record.value() == null) {
            throw new IllegalArgumentException("Получено пустое событие датчика");
        }

        SensorEventAvro event = reader.read(
                null,
                DecoderFactory.get().binaryDecoder(record.value(), null)
        );

        // Для учебного примера обработка — вывод события в лог.
        log.info("topic={}, partition={}, offset={}, key={}, event={}",
                record.topic(), record.partition(), record.offset(),
                record.key(), event);
    }

    private static void commitAsync(
            KafkaConsumer<String, byte[]> consumer,
            Map<TopicPartition, OffsetAndMetadata> currentOffsets
    ) {
        consumer.commitAsync(new HashMap<>(currentOffsets), (offsets, exception) -> {
            if (exception != null) {
                log.warn("Ошибка фиксации смещений: {}", offsets, exception);
            }
        });
    }

    private static Properties getConsumerProperties() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "sensor-consumer");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "sensor-consumer-group");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class);

        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        // 100 записей * 3 секунды + 30 секунд запаса.
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 330_000);
        properties.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 3_072_000);
        properties.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 307_200);
        return properties;
    }
}