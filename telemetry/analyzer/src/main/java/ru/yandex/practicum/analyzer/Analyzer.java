package ru.yandex.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.analyzer.processor.HubEventProcessor;
import ru.yandex.practicum.analyzer.processor.SnapshotProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {

    public static void main(String[] args) {

        try (ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args)) {
            HubEventProcessor hubEventProcessor =
                    context.getBean(HubEventProcessor.class);

            SnapshotProcessor snapshotProcessor =
                    context.getBean(SnapshotProcessor.class);

            hubEventProcessor.start(snapshotProcessor::stop);
            snapshotProcessor.start();
        }
    }
}