package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.LightSensorEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

import java.time.Instant;

@Component
public class LightSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    public LightSensorEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.LIGHT_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        LightSensorProto lightSensor = event.getLightSensor();

        LightSensorEvent sensorEvent = new LightSensorEvent();
        sensorEvent.setId(event.getId());
        sensorEvent.setHubId(event.getHubId());
        sensorEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        sensorEvent.setLinkQuality(lightSensor.getLinkQuality());
        sensorEvent.setLuminosity(lightSensor.getLuminosity());

        eventService.collectSensorEvent(sensorEvent);
    }
}