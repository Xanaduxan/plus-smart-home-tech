package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.DeviceAddedEvent;
import ru.yandex.practicum.collector.model.hub.DeviceType;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.DeviceAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

import java.time.Instant;

@Component
public class DeviceAddedEventHandler implements HubEventHandler {

    private final EventService eventService;

    public DeviceAddedEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceAddedEventProto device = event.getDeviceAdded();

        DeviceAddedEvent hubEvent = new DeviceAddedEvent();
        hubEvent.setHubId(event.getHubId());
        hubEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        hubEvent.setId(device.getId());
        hubEvent.setDeviceType(DeviceType.valueOf(device.getType().name()));

        eventService.collectHubEvent(hubEvent);
    }
}