package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.ClimateSensorEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

import java.time.Instant;

@Component
public class ClimateSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    public ClimateSensorEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        ClimateSensorProto climateSensor = event.getClimateSensor();

        ClimateSensorEvent sensorEvent = new ClimateSensorEvent();
        sensorEvent.setId(event.getId());
        sensorEvent.setHubId(event.getHubId());
        sensorEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        sensorEvent.setTemperatureC(climateSensor.getTemperatureC());
        sensorEvent.setHumidity(climateSensor.getHumidity());
        sensorEvent.setCo2Level(climateSensor.getCo2Level());

        eventService.collectSensorEvent(sensorEvent);
    }
}