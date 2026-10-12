package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;

@Service
public class SnapshotService {

    private final ScenarioEvaluator scenarioEvaluator;
    private final DeviceActionService deviceActionService;

    public SnapshotService(
            ScenarioEvaluator scenarioEvaluator,
            DeviceActionService deviceActionService
    ) {
        this.scenarioEvaluator = scenarioEvaluator;
        this.deviceActionService = deviceActionService;
    }

    public void process(SensorsSnapshotAvro snapshot) {
        List<ActionToExecute> actions = scenarioEvaluator
                .findActions(snapshot);

        for (ActionToExecute action : actions) {
            deviceActionService.send(
                    snapshot.getHubId(),
                    action,
                    snapshot.getTimestamp()
            );
        }
    }
}