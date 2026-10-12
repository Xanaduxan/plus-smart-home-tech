package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class ScenarioAction {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    protected ScenarioAction() {
    }

    public ScenarioAction(Sensor sensor, Action action) {
        this.sensor = sensor;
        this.action = action;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public Action getAction() {
        return action;
    }
}