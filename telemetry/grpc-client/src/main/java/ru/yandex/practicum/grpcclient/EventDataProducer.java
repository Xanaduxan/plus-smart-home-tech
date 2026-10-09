package ru.yandex.practicum.grpcclient;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import com.google.protobuf.Timestamp;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;
import org.springframework.scheduling.annotation.Scheduled;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SwitchSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import java.util.concurrent.ThreadLocalRandom;
import com.google.protobuf.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventDataProducer {
    private final SensorProperties sensorProperties;
    private static final Logger log =
            LoggerFactory.getLogger(EventDataProducer.class);
    private final Map<String, Integer> temperatures = new HashMap<>();
    private int getRandomSensorValue(int prevValue) {
        int change = (int) (Math.random() * 3) - 1;
        return prevValue + change;
    }

    private SensorEventProto createTemperatureSensorEvent(
            SensorProperties.TemperatureSensor sensor
    ) {
        SensorProperties.ValueRange range = sensor.temperature();

        int previous = temperatures.getOrDefault(
                sensor.id(),
                (int) (((long) range.minValue() + range.maxValue()) / 2)
        );

        int temperatureCelsius = Math.clamp(
                getRandomSensorValue(previous),
                range.minValue(),
                range.maxValue()
        );
        temperatures.put(sensor.id(), temperatureCelsius);

        int temperatureFahrenheit = (int) (temperatureCelsius * 1.8 + 32);
        Instant ts = Instant.now();

        return SensorEventProto.newBuilder()
                .setId(sensor.id())
                .setHubId(sensorProperties.hubId())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(ts.getEpochSecond())
                        .setNanos(ts.getNano())
                        .build())
                .setTemperatureSensor(TemperatureSensorProto.newBuilder()
                        .setTemperatureC(temperatureCelsius)
                        .setTemperatureF(temperatureFahrenheit)
                        .build())
                .build();
    }
    public EventDataProducer(SensorProperties sensorProperties) {
        this.sensorProperties = sensorProperties;
    }

    private void sendEvent(SensorEventProto event) {
        log.info("Отправляю данные: {}", event.getAllFields());

        Empty response = collectorStub.collectSensorEvent(event);

        log.info("Получил ответ от коллектора: {}", response);
    }

    private int randomInRange(SensorProperties.ValueRange range) {
        return (int) ThreadLocalRandom.current().nextLong(
                range.minValue(),
                (long) range.maxValue() + 1
        );
    }

    private SensorEventProto createMotionSensorEvent(
            SensorProperties.MotionSensor sensor
    ) {
        Instant ts = Instant.now();

        return SensorEventProto.newBuilder()
                .setId(sensor.id())
                .setHubId(sensorProperties.hubId())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(ts.getEpochSecond())
                        .setNanos(ts.getNano())
                        .build())
                .setMotionSensor(MotionSensorProto.newBuilder()
                        .setLinkQuality(randomInRange(sensor.linkQuality()))
                        .setVoltage(randomInRange(sensor.voltage()))
                        .setMotion(ThreadLocalRandom.current().nextBoolean())
                        .build())
                .build();
    }
    private SensorEventProto createSwitchSensorEvent(
            SensorProperties.SwitchSensor sensor
    ) {
        Instant ts = Instant.now();

        return SensorEventProto.newBuilder()
                .setId(sensor.id())
                .setHubId(sensorProperties.hubId())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(ts.getEpochSecond())
                        .setNanos(ts.getNano())
                        .build())
                .setSwitchSensor(SwitchSensorProto.newBuilder()
                        .setState(ThreadLocalRandom.current().nextBoolean())
                        .build())
                .build();
    }

    private SensorEventProto createLightSensorEvent(
            SensorProperties.LightSensor sensor
    ) {
        Instant ts = Instant.now();

        return SensorEventProto.newBuilder()
                .setId(sensor.id())
                .setHubId(sensorProperties.hubId())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(ts.getEpochSecond())
                        .setNanos(ts.getNano())
                        .build())
                .setLightSensor(LightSensorProto.newBuilder()
                        .setLuminosity(randomInRange(sensor.luminosity()))
                        .build())
                .build();
    }

    private SensorEventProto createClimateSensorEvent(
            SensorProperties.ClimateSensor sensor
    ) {
        Instant ts = Instant.now();

        return SensorEventProto.newBuilder()
                .setId(sensor.id())
                .setHubId(sensorProperties.hubId())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(ts.getEpochSecond())
                        .setNanos(ts.getNano())
                        .build())
                .setClimateSensor(ClimateSensorProto.newBuilder()
                        .setTemperatureC(randomInRange(sensor.temperature()))
                        .setHumidity(randomInRange(sensor.humidity()))
                        .setCo2Level(randomInRange(sensor.co2Level()))
                        .build())
                .build();
    }

    @Scheduled(initialDelay = 1000, fixedDelay = 1000)
    public void sendSensorEvents() {
        for (SensorProperties.TemperatureSensor sensor
                : sensorProperties.temperatureSensors()) {
            sendEvent(createTemperatureSensorEvent(sensor));
        }

        for (SensorProperties.MotionSensor sensor
                : sensorProperties.motionSensors()) {
            sendEvent(createMotionSensorEvent(sensor));
        }
        for (SensorProperties.SwitchSensor sensor
                : sensorProperties.switchSensors()) {
            sendEvent(createSwitchSensorEvent(sensor));
        }
        for (SensorProperties.LightSensor sensor
                : sensorProperties.lightSensors()) {
            sendEvent(createLightSensorEvent(sensor));
        }

        for (SensorProperties.ClimateSensor sensor
                : sensorProperties.climateSensors()) {
            sendEvent(createClimateSensorEvent(sensor));
        }
    }
    @GrpcClient("collector")
    private CollectorControllerGrpc.CollectorControllerBlockingStub collectorStub;
}