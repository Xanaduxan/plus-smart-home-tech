package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.ScenarioRemovedEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

import java.time.Instant;

@Component
public class ScenarioRemovedEventHandler implements HubEventHandler {

    private final EventService eventService;

    public ScenarioRemovedEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioRemovedEvent hubEvent = new ScenarioRemovedEvent();
        hubEvent.setHubId(event.getHubId());
        hubEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        hubEvent.setName(event.getScenarioRemoved().getName());

        eventService.collectHubEvent(hubEvent);
    }
}