package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class ScenarioCondition {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", nullable = false)
    private Condition condition;

    protected ScenarioCondition() {
    }

    public ScenarioCondition(Sensor sensor, Condition condition) {
        this.sensor = sensor;
        this.condition = condition;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public Condition getCondition() {
        return condition;
    }
}