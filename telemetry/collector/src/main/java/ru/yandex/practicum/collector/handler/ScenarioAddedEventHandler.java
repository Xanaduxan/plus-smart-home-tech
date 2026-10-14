package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.ActionType;
import ru.yandex.practicum.collector.model.hub.ConditionOperation;
import ru.yandex.practicum.collector.model.hub.ConditionType;
import ru.yandex.practicum.collector.model.hub.DeviceAction;
import ru.yandex.practicum.collector.model.hub.ScenarioAddedEvent;
import ru.yandex.practicum.collector.model.hub.ScenarioCondition;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;

import java.time.Instant;

@Component
public class ScenarioAddedEventHandler implements HubEventHandler {

    private final EventService eventService;

    public ScenarioAddedEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioAddedEventProto scenario = event.getScenarioAdded();

        ScenarioAddedEvent hubEvent = new ScenarioAddedEvent();
        hubEvent.setHubId(event.getHubId());
        hubEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        hubEvent.setName(scenario.getName());
        hubEvent.setConditions(scenario.getConditionsList().stream()
                .map(this::toCondition)
                .toList());
        hubEvent.setActions(scenario.getActionsList().stream()
                .map(this::toAction)
                .toList());

        eventService.collectHubEvent(hubEvent);
    }

    private ScenarioCondition toCondition(ScenarioConditionProto source) {
        ScenarioCondition condition = new ScenarioCondition();
        condition.setSensorId(source.getSensorId());
        condition.setType(ConditionType.valueOf(source.getType().name()));
        condition.setOperation(
                ConditionOperation.valueOf(source.getOperation().name())
        );

        Object value = switch (source.getValueCase()) {
            case BOOL_VALUE -> source.getBoolValue();
            case INT_VALUE -> source.getIntValue();
            case VALUE_NOT_SET -> null;
        };
        condition.setValue(value);

        return condition;
    }

    private DeviceAction toAction(DeviceActionProto source) {
        DeviceAction action = new DeviceAction();
        action.setSensorId(source.getSensorId());
        action.setType(ActionType.valueOf(source.getType().name()));
        action.setValue(source.hasValue() ? source.getValue() : null);

        return action;
    }
}