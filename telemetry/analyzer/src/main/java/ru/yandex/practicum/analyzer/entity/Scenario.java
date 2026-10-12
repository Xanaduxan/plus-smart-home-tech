package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "scenarios",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"hub_id", "name"}
        )
)
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hub_id")
    private String hubId;

    private String name;

    @ElementCollection
    @CollectionTable(
            name = "scenario_conditions",
            joinColumns = @JoinColumn(name = "scenario_id")
    )
    private List<ScenarioCondition> conditions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "scenario_actions",
            joinColumns = @JoinColumn(name = "scenario_id")
    )
    private List<ScenarioAction> actions = new ArrayList<>();

    protected Scenario() {
    }

    public Scenario(String hubId, String name) {
        this.hubId = hubId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getHubId() {
        return hubId;
    }

    public String getName() {
        return name;
    }

    public List<ScenarioCondition> getConditions() {
        return conditions;
    }

    public List<ScenarioAction> getActions() {
        return actions;
    }
}