package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.DeviceRemovedEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

import java.time.Instant;

@Component
public class DeviceRemovedEventHandler implements HubEventHandler {

    private final EventService eventService;

    public DeviceRemovedEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceRemovedEvent hubEvent = new DeviceRemovedEvent();
        hubEvent.setHubId(event.getHubId());
        hubEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        hubEvent.setId(event.getDeviceRemoved().getId());

        eventService.collectHubEvent(hubEvent);
    }
}