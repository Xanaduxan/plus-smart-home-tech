package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

@Component
public class SensorValueExtractor {

    public Integer extract(ConditionTypeAvro type, Object data) {
        if (type == null || data == null) {
            return null;
        }

        return switch (type) {
            case MOTION -> data instanceof MotionSensorAvro sensor
                    ? (sensor.getMotion() ? 1 : 0)
                    : null;

            case LUMINOSITY -> data instanceof LightSensorAvro sensor
                    ? sensor.getLuminosity()
                    : null;

            case SWITCH -> data instanceof SwitchSensorAvro sensor
                    ? (sensor.getState() ? 1 : 0)
                    : null;

            case TEMPERATURE -> switch (data) {
                case TemperatureSensorAvro sensor ->
                        sensor.getTemperatureC();

                case ClimateSensorAvro sensor ->
                        sensor.getTemperatureC();

                default -> null;
            };

            case CO2LEVEL -> data instanceof ClimateSensorAvro sensor
                    ? sensor.getCo2Level()
                    : null;

            case HUMIDITY -> data instanceof ClimateSensorAvro sensor
                    ? sensor.getHumidity()
                    : null;
        };
    }
}