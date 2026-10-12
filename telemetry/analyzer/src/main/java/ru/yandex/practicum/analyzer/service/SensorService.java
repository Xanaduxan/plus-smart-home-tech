package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.entity.Sensor;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;

import java.util.List;

@Service
@Transactional
public class SensorService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioService scenarioService;

    public SensorService(
            SensorRepository sensorRepository,
            ScenarioRepository scenarioRepository,
            ScenarioService scenarioService
    ) {
        this.sensorRepository = sensorRepository;
        this.scenarioRepository = scenarioRepository;
        this.scenarioService = scenarioService;
    }

    public void add(String hubId, String sensorId) {
        Sensor existing = sensorRepository.findById(sensorId)
                .orElse(null);

        if (existing != null) {
            if (!hubId.equals(existing.getHubId())) {
                throw new IllegalArgumentException(
                        "Устройство " + sensorId
                                + " уже зарегистрировано в другом хабе"
                );
            }

            return;
        }

        sensorRepository.save(new Sensor(sensorId, hubId));
    }

    public void remove(String hubId, String sensorId) {
        Sensor sensor = sensorRepository
                .findByIdAndHubId(sensorId, hubId)
                .orElse(null);

        if (sensor == null) {
            return;
        }

        List<Scenario> relatedScenarios = scenarioRepository
                .findByHubId(hubId)
                .stream()
                .filter(scenario -> referencesSensor(scenario, sensorId))
                .toList();

        relatedScenarios.forEach(scenarioService::remove);

        scenarioRepository.flush();

        sensorRepository.delete(sensor);
    }

    private boolean referencesSensor(Scenario scenario, String sensorId) {
        return scenario.getConditions().stream()
                .anyMatch(link -> sensorId.equals(link.getSensor().getId()))
                || scenario.getActions().stream()
                .anyMatch(link -> sensorId.equals(link.getSensor().getId()));
    }
}