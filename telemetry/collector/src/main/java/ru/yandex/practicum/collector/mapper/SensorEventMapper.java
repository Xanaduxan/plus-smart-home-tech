package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEvent event) {
        Object payload = switch (event) {
            case ClimateSensorEvent climate ->
                    ClimateSensorAvro.newBuilder()
                            .setTemperatureC(climate.getTemperatureC())
                            .setHumidity(climate.getHumidity())
                            .setCo2Level(climate.getCo2Level())
                            .build();

            case LightSensorEvent light ->
                    LightSensorAvro.newBuilder()
                            .setLinkQuality(light.getLinkQuality())
                            .setLuminosity(light.getLuminosity())
                            .build();

            case MotionSensorEvent motion ->
                    MotionSensorAvro.newBuilder()
                            .setLinkQuality(motion.getLinkQuality())
                            .setMotion(motion.getMotion())
                            .setVoltage(motion.getVoltage())
                            .build();

            case SwitchSensorEvent switchEvent ->
                    SwitchSensorAvro.newBuilder()
                            .setState(switchEvent.getState())
                            .build();

            case TemperatureSensorEvent temperature ->
                    TemperatureSensorAvro.newBuilder()
                            .setId(temperature.getId())
                            .setHubId(temperature.getHubId())
                            .setTimestamp(temperature.getTimestamp())
                            .setTemperatureC(temperature.getTemperatureC())
                            .setTemperatureF(temperature.getTemperatureF())
                            .build();

            default -> throw new IllegalArgumentException(
                    "Неподдерживаемый тип события датчика: "
                            + event.getType()
                            + ", класс: "
                            + event.getClass().getName()
            );
        };

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }
}