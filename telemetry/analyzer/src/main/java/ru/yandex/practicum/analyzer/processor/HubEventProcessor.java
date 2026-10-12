package ru.yandex.practicum.analyzer.processor;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.kafka.AnalyzerKafkaProperties;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.util.List;

@Component
public class HubEventProcessor implements Runnable {

    private static final Logger log =
            LoggerFactory.getLogger(HubEventProcessor.class);

    private final Consumer<String, HubEventAvro> consumer;
    private final AnalyzerKafkaProperties properties;
    private final HubEventService hubEventService;

    private volatile boolean running = true;
    private Thread processingThread;

    public HubEventProcessor(
            @Qualifier("hubEventConsumer")
            Consumer<String, HubEventAvro> consumer,
            AnalyzerKafkaProperties properties,
            HubEventService hubEventService
    ) {
        this.consumer = consumer;
        this.properties = properties;
        this.hubEventService = hubEventService;
    }

    public synchronized void start(Runnable onFailure) {
        if (processingThread != null || !running) {
            throw new IllegalStateException(
                    "Обработчик событий хаба нельзя запустить повторно"
            );
        }

        processingThread = new Thread(this, "HubEventHandlerThread");

        processingThread.setUncaughtExceptionHandler((thread, exception) -> {
            log.error("Ошибка обработки событий хаба", exception);
            onFailure.run();
        });

        processingThread.start();
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of(properties.hubs().topic()));

            while (running) {
                ConsumerRecords<String, HubEventAvro> records =
                        consumer.poll(properties.pollTimeout());

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    HubEventAvro event = record.value();

                    if (event == null) {
                        throw new IllegalArgumentException(
                                "Получено пустое событие хаба"
                        );
                    }

                    hubEventService.handle(event);
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException exception) {
            if (running) {
                throw exception;
            }
        } finally {
            running = false;
            consumer.close();
        }
    }

    @PreDestroy
    public void stop() {
        Thread thread;

        synchronized (this) {
            running = false;
            thread = processingThread;

            if (thread == null) {
                consumer.close();
                return;
            }

            consumer.wakeup();
        }

        if (thread != Thread.currentThread()) {
            try {
                thread.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}