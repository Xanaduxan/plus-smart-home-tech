package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.Action;
import ru.yandex.practicum.analyzer.entity.Condition;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.entity.ScenarioAction;
import ru.yandex.practicum.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.analyzer.entity.Sensor;
import ru.yandex.practicum.analyzer.repository.ActionRepository;
import ru.yandex.practicum.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    public ScenarioService(
            ScenarioRepository scenarioRepository,
            SensorRepository sensorRepository,
            ConditionRepository conditionRepository,
            ActionRepository actionRepository
    ) {
        this.scenarioRepository = scenarioRepository;
        this.sensorRepository = sensorRepository;
        this.conditionRepository = conditionRepository;
        this.actionRepository = actionRepository;
    }

    public void add(String hubId, ScenarioAddedEventAvro event) {
        Set<String> sensorIds = Stream.concat(
                event.getConditions().stream()
                        .map(ScenarioConditionAvro::getSensorId),
                event.getActions().stream()
                        .map(DeviceActionAvro::getSensorId)
        ).collect(Collectors.toSet());

        Map<String, Sensor> sensors = sensorRepository
                .findByIdInAndHubId(sensorIds, hubId)
                .stream()
                .collect(Collectors.toMap(
                        Sensor::getId,
                        sensor -> sensor
                ));

        if (sensors.size() != sensorIds.size()) {
            throw new IllegalArgumentException(
                    "Не все устройства сценария зарегистрированы в хабе " + hubId
            );
        }

        Scenario scenario = scenarioRepository
                .findByHubIdAndName(hubId, event.getName())
                .orElseGet(() -> new Scenario(hubId, event.getName()));

        if (scenario.getId() != null) {
            clearScenarioData(scenario);
        }

        scenario = scenarioRepository.save(scenario);

        for (ScenarioConditionAvro conditionEvent : event.getConditions()) {
            Condition condition = conditionRepository.save(
                    new Condition(
                            conditionEvent.getType(),
                            conditionEvent.getOperation(),
                            toInteger(conditionEvent.getValue())
                    )
            );

            scenario.getConditions().add(
                    new ScenarioCondition(
                            sensors.get(conditionEvent.getSensorId()),
                            condition
                    )
            );
        }

        for (DeviceActionAvro actionEvent : event.getActions()) {
            Action action = actionRepository.save(
                    new Action(
                            actionEvent.getType(),
                            actionEvent.getValue()
                    )
            );

            scenario.getActions().add(
                    new ScenarioAction(
                            sensors.get(actionEvent.getSensorId()),
                            action
                    )
            );
        }
    }

    public void remove(String hubId, String name) {
        scenarioRepository.findByHubIdAndName(hubId, name)
                .ifPresent(this::remove);
    }

    public void remove(Scenario scenario) {
        clearScenarioData(scenario);
        scenarioRepository.delete(scenario);
    }

    private void clearScenarioData(Scenario scenario) {
        List<Condition> conditions = scenario.getConditions().stream()
                .map(ScenarioCondition::getCondition)
                .toList();

        List<Action> actions = scenario.getActions().stream()
                .map(ScenarioAction::getAction)
                .toList();

        scenario.getConditions().clear();
        scenario.getActions().clear();


        scenarioRepository.flush();

        conditionRepository.deleteAll(conditions);
        actionRepository.deleteAll(actions);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? 1 : 0;
        }

        return (Integer) value;
    }
}