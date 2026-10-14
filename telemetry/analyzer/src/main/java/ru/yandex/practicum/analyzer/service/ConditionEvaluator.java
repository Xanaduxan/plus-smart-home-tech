package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.Condition;
import ru.yandex.practicum.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Component
public class ConditionEvaluator {

    private final SensorValueExtractor sensorValueExtractor;

    public ConditionEvaluator(SensorValueExtractor sensorValueExtractor) {
        this.sensorValueExtractor = sensorValueExtractor;
    }

    public boolean matches(
            ScenarioCondition scenarioCondition,
            SensorsSnapshotAvro snapshot
    ) {
        Condition condition = scenarioCondition.getCondition();

        if (condition.getType() == null
                || condition.getOperation() == null
                || condition.getValue() == null) {
            return false;
        }

        SensorStateAvro state = snapshot.getSensorsState()
                .get(scenarioCondition.getSensor().getId());

        if (state == null) {
            return false;
        }

        Integer actualValue = sensorValueExtractor.extract(
                condition.getType(),
                state.getData()
        );

        if (actualValue == null) {
            return false;
        }

        int actual = actualValue;
        int expected = condition.getValue();

        return switch (condition.getOperation()) {
            case EQUALS -> actual == expected;
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }
}