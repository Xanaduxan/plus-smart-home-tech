package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.MotionSensorEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

import java.time.Instant;

@Component
public class MotionSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    public MotionSensorEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.MOTION_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        MotionSensorProto motionSensor = event.getMotionSensor();

        MotionSensorEvent sensorEvent = new MotionSensorEvent();
        sensorEvent.setId(event.getId());
        sensorEvent.setHubId(event.getHubId());
        sensorEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        sensorEvent.setLinkQuality(motionSensor.getLinkQuality());
        sensorEvent.setMotion(motionSensor.getMotion());
        sensorEvent.setVoltage(motionSensor.getVoltage());

        eventService.collectSensorEvent(sensorEvent);
    }
}