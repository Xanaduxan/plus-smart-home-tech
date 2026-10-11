package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private static final String SENSORS_TOPIC = "telemetry.sensors.v1";
    private static final String SNAPSHOTS_TOPIC = "telemetry.snapshots.v1";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);

    private final Consumer<String, SensorEventAvro> consumer;
    private final Producer<String, SpecificRecordBase> producer;
    private final SnapshotAggregator snapshotAggregator;

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets =
            new HashMap<>();

    public void start() {
        CountDownLatch closed = new CountDownLatch(1);

        Thread shutdownHook = new Thread(() -> {
            consumer.wakeup();

            try {
                closed.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "aggregator-shutdown");

        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            consumer.subscribe(List.of(SENSORS_TOPIC));

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(POLL_TIMEOUT);

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    handleRecord(record);

                    // Учитываем только успешно обработанные события.
                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                }

                commitOffsets();
            }
        } catch (WakeupException ignored) {
            log.info("Останавливаем Aggregator");
        } catch (Exception exception) {
            log.error(
                    "Ошибка во время обработки событий от датчиков",
                    exception
            );
        } finally {
            try {
                producer.flush();
                commitOffsets();
            } catch (Exception exception) {
                log.error(
                        "Ошибка при завершении обработки данных",
                        exception
                );
            } finally {
                try {
                    closeClients();
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

    private void handleRecord(
            ConsumerRecord<String, SensorEventAvro> record
    ) throws ExecutionException, InterruptedException {
        SensorEventAvro event = record.value();

        if (event == null) {
            throw new IllegalArgumentException("Получено пустое событие датчика");
        }

        Optional<SensorsSnapshotAvro> result =
                snapshotAggregator.updateState(event);

        if (result.isPresent()) {
            SensorsSnapshotAvro snapshot = result.get();

            ProducerRecord<String, SpecificRecordBase> snapshotRecord =
                    new ProducerRecord<>(
                            SNAPSHOTS_TOPIC,
                            snapshot.getHubId(),
                            snapshot
                    );

            // Дожидаемся подтверждения отправки.
            producer.send(snapshotRecord).get();
        }
    }

    private void commitOffsets() {
        if (!currentOffsets.isEmpty()) {
            consumer.commitSync(new HashMap<>(currentOffsets));
            currentOffsets.clear();
        }
    }

    private void closeClients() {
        try {
            log.info("Закрываем консьюмер");
            consumer.close();
        } finally {
            log.info("Закрываем продюсер");
            producer.close();
        }
    }
}
