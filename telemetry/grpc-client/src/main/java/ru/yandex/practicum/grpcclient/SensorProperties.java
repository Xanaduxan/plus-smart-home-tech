package ru.yandex.practicum.grpcclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sensor")
public record SensorProperties(
        String hubId,
        List<MotionSensor> motionSensors,
        List<SwitchSensor> switchSensors,
        List<TemperatureSensor> temperatureSensors,
        List<LightSensor> lightSensors,
        List<ClimateSensor> climateSensors
) {

    public record ValueRange(int minValue, int maxValue) {
    }

    public record MotionSensor(
            String id,
            ValueRange linkQuality,
            ValueRange voltage
    ) {
    }

    public record SwitchSensor(String id) {
    }

    public record TemperatureSensor(
            String id,
            ValueRange temperature
    ) {
    }

    public record LightSensor(
            String id,
            ValueRange luminosity
    ) {
    }

    public record ClimateSensor(
            String id,
            ValueRange temperature,
            ValueRange humidity,
            ValueRange co2Level
    ) {
    }
}