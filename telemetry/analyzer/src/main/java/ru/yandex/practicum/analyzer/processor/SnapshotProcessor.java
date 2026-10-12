package ru.yandex.practicum.analyzer.processor;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.kafka.AnalyzerKafkaProperties;
import ru.yandex.practicum.analyzer.service.SnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Component
public class SnapshotProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(SnapshotProcessor.class);

    private final Consumer<String, SensorsSnapshotAvro> consumer;
    private final AnalyzerKafkaProperties properties;
    private final SnapshotService snapshotService;

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets =
            new HashMap<>();

    private final CountDownLatch closed = new CountDownLatch(1);

    private volatile boolean running = true;
    private Thread processingThread;

    public SnapshotProcessor(
            @Qualifier("snapshotConsumer")
            Consumer<String, SensorsSnapshotAvro> consumer,
            AnalyzerKafkaProperties properties,
            SnapshotService snapshotService
    ) {
        this.consumer = consumer;
        this.properties = properties;
        this.snapshotService = snapshotService;
    }

    public void start() {
        synchronized (this) {
            if (processingThread != null || !running) {
                throw new IllegalStateException(
                        "Обработчик снапшотов нельзя запустить повторно"
                );
            }

            processingThread = Thread.currentThread();
        }

        try {
            consumer.subscribe(List.of(properties.snapshots().topic()));

            while (running) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        consumer.poll(properties.pollTimeout());

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    if (!running) {
                        break;
                    }

                    SensorsSnapshotAvro snapshot = record.value();

                    if (snapshot == null) {
                        throw new IllegalArgumentException(
                                "Получен пустой снапшот"
                        );
                    }

                    snapshotService.process(snapshot);

                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );

                    commitOffsets();
                }
            }
        } catch (WakeupException exception) {
            if (running) {
                throw exception;
            }
        } catch (RuntimeException exception) {
            log.error("Ошибка обработки снапшотов", exception);
            throw exception;
        } finally {
            running = false;

            try {
                commitOffsets();
            } catch (RuntimeException exception) {
                log.error(
                        "Не удалось зафиксировать смещения при закрытии",
                        exception
                );
            } finally {
                try {
                    consumer.close();
                } finally {
                    closed.countDown();
                }
            }
        }
    }

    private void commitOffsets() {
        if (!currentOffsets.isEmpty()) {
            consumer.commitSync(new HashMap<>(currentOffsets));
            currentOffsets.clear();
        }
    }

    @PreDestroy
    public void stop() {
        Thread thread;

        synchronized (this) {
            running = false;
            thread = processingThread;

            if (thread == null) {
                try {
                    consumer.close();
                } finally {
                    closed.countDown();
                }

                return;
            }

            consumer.wakeup();
        }

        if (thread != Thread.currentThread()) {
            try {
                closed.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}