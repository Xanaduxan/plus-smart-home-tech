package ru.yandex.practicum.analyzer.service;

import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;

public record ActionToExecute(
        String scenarioName,
        String sensorId,
        ActionTypeAvro type,
        Integer value
) {
}