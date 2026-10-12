package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.Action;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.entity.ScenarioAction;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;

@Component
public class ScenarioEvaluator {

    private final ConditionEvaluator conditionEvaluator;
    private final ScenarioRepository scenarioRepository;

    public ScenarioEvaluator(
            ConditionEvaluator conditionEvaluator,
            ScenarioRepository scenarioRepository
    ) {
        this.conditionEvaluator = conditionEvaluator;
        this.scenarioRepository = scenarioRepository;
    }

    @Transactional
    public List<ActionToExecute> findActions(SensorsSnapshotAvro snapshot) {
        List<Scenario> scenarios = scenarioRepository
                .findByHubId(snapshot.getHubId());

        return evaluate(snapshot, scenarios);
    }

    public List<ActionToExecute> evaluate(
            SensorsSnapshotAvro snapshot,
            List<Scenario> scenarios
    ) {
        return scenarios.stream()
                .filter(scenario ->
                        snapshot.getHubId().equals(scenario.getHubId()))
                .filter(scenario ->
                        scenario.getConditions().stream()
                                .allMatch(condition ->
                                        conditionEvaluator.matches(
                                                condition,
                                                snapshot
                                        )))
                .flatMap(scenario ->
                        scenario.getActions().stream()
                                .map(scenarioAction ->
                                        toActionToExecute(
                                                scenario.getName(),
                                                scenarioAction
                                        )))
                .toList();
    }

    private ActionToExecute toActionToExecute(
            String scenarioName,
            ScenarioAction scenarioAction
    ) {
        Action action = scenarioAction.getAction();

        return new ActionToExecute(
                scenarioName,
                scenarioAction.getSensor().getId(),
                action.getType(),
                action.getValue()
        );
    }
}