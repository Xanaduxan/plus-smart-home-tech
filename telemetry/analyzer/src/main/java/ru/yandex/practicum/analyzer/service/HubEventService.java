package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

@Service
public class HubEventService {

    private final SensorService sensorService;
    private final ScenarioService scenarioService;

    public HubEventService(
            SensorService sensorService,
            ScenarioService scenarioService
    ) {
        this.sensorService = sensorService;
        this.scenarioService = scenarioService;
    }

    public void handle(HubEventAvro event) {
        String hubId = event.getHubId();

        switch (event.getPayload()) {
            case DeviceAddedEventAvro payload ->
                    sensorService.add(hubId, payload.getId());

            case DeviceRemovedEventAvro payload ->
                    sensorService.remove(hubId, payload.getId());

            case ScenarioAddedEventAvro payload ->
                    scenarioService.add(hubId, payload);

            case ScenarioRemovedEventAvro payload ->
                    scenarioService.remove(hubId, payload.getName());

            default -> throw new IllegalArgumentException(
                    "Неизвестный тип события хаба: " + event.getPayload()
            );
        }
    }
}