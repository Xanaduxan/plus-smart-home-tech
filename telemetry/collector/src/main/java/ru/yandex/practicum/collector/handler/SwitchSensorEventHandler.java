package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.SwitchSensorEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

import java.time.Instant;

@Component
public class SwitchSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    public SwitchSensorEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        SwitchSensorEvent sensorEvent = new SwitchSensorEvent();
        sensorEvent.setId(event.getId());
        sensorEvent.setHubId(event.getHubId());
        sensorEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        sensorEvent.setState(event.getSwitchSensor().getState());

        eventService.collectSensorEvent(sensorEvent);
    }
}