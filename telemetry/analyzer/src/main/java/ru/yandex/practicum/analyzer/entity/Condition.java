package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

@Entity
@Table(name = "conditions")
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ConditionTypeAvro type;

    @Enumerated(EnumType.STRING)
    private ConditionOperationAvro operation;

    private Integer value;

    protected Condition() {
    }

    public Condition(
            ConditionTypeAvro type,
            ConditionOperationAvro operation,
            Integer value
    ) {
        this.type = type;
        this.operation = operation;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public ConditionTypeAvro getType() {
        return type;
    }

    public ConditionOperationAvro getOperation() {
        return operation;
    }

    public Integer getValue() {
        return value;
    }
}