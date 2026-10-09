package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.TemperatureSensorEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;

import java.time.Instant;

@Component
public class TemperatureSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    public TemperatureSensorEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.TEMPERATURE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        TemperatureSensorProto temperatureSensor =
                event.getTemperatureSensor();

        TemperatureSensorEvent sensorEvent = new TemperatureSensorEvent();
        sensorEvent.setId(event.getId());
        sensorEvent.setHubId(event.getHubId());
        sensorEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        sensorEvent.setTemperatureC(temperatureSensor.getTemperatureC());
        sensorEvent.setTemperatureF(temperatureSensor.getTemperatureF());

        eventService.collectSensorEvent(sensorEvent);
    }
}