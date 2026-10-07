package ru.yandex.practicum.collector.service;

import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;

public interface EventService {

    void collectSensorEvent(SensorEvent event);

    void collectHubEvent(HubEvent event);
}
