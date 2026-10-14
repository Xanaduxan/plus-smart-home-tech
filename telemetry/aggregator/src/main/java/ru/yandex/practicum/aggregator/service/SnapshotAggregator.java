package ru.yandex.practicum.aggregator.service;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class SnapshotAggregator {

    // Ключ — идентификатор хаба, значение — его текущий снапшот.
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        SensorsSnapshotAvro snapshot = snapshots.get(event.getHubId());

        // Если снапшота хаба ещё нет, создаём и сохраняем его.
        if (snapshot == null) {
            snapshot = SensorsSnapshotAvro.newBuilder()
                    .setHubId(event.getHubId())
                    .setTimestamp(event.getTimestamp())
                    .setSensorsState(new HashMap<>())
                    .build();

            snapshots.put(event.getHubId(), snapshot);
        }

        SensorStateAvro oldState =
                snapshot.getSensorsState().get(event.getId());

        // Старые события и повторные показания игнорируем.
        if (oldState != null) {
            boolean outdatedEvent =
                    oldState.getTimestamp().isAfter(event.getTimestamp());

            boolean sameData =
                    isSameData(oldState.getData(), event.getPayload());

            if (outdatedEvent || sameData) {
                return Optional.empty();
            }
        }

        // Добавляем новый датчик или заменяем его прежнее состояние.
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        snapshot.getSensorsState().put(event.getId(), newState);
        snapshot.setTimestamp(event.getTimestamp());

        return Optional.of(snapshot);
    }

    private boolean isSameData(Object oldData, Object newData) {
        if (oldData instanceof TemperatureSensorAvro oldTemperature
                && newData instanceof TemperatureSensorAvro newTemperature) {

            // Сравниваем показания температуры без метки времени события.
            return oldTemperature.getTemperatureC()
                    == newTemperature.getTemperatureC()
                    && oldTemperature.getTemperatureF()
                    == newTemperature.getTemperatureF();
        }

        return Objects.equals(oldData, newData);
    }
}
