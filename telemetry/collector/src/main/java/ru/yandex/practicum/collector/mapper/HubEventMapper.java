package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent event) {
        Object payload = switch (event) {
            case DeviceAddedEvent added ->
                    DeviceAddedEventAvro.newBuilder()
                            .setId(added.getId())
                            .setType(DeviceTypeAvro.valueOf(
                                    added.getDeviceType().name()
                            ))
                            .build();

            case DeviceRemovedEvent removed ->
                    DeviceRemovedEventAvro.newBuilder()
                            .setId(removed.getId())
                            .build();

            case ScenarioAddedEvent added ->
                    ScenarioAddedEventAvro.newBuilder()
                            .setName(added.getName())
                            .setConditions(added.getConditions().stream()
                                    .map(this::toAvro)
                                    .toList())
                            .setActions(added.getActions().stream()
                                    .map(this::toAvro)
                                    .toList())
                            .build();

            case ScenarioRemovedEvent removed ->
                    ScenarioRemovedEventAvro.newBuilder()
                            .setName(removed.getName())
                            .build();

            default -> throw new IllegalArgumentException(
                    "Неподдерживаемый тип события хаба: "
                            + event.getType()
                            + ", класс: "
                            + event.getClass().getName()
            );
        };

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }

    private ScenarioConditionAvro toAvro(ScenarioCondition condition) {
        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(
                        condition.getType().name()
                ))
                .setOperation(ConditionOperationAvro.valueOf(
                        condition.getOperation().name()
                ))
                .setValue(condition.getValue())
                .build();
    }

    private DeviceActionAvro toAvro(DeviceAction action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(
                        action.getType().name()
                ))
                .setValue(action.getValue())
                .build();
    }
}