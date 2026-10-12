package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceActionService {

    @GrpcClient("hub-router")
    private HubRouterControllerBlockingStub hubRouterClient;

    public void send(
            String hubId,
            ActionToExecute action,
            Instant timestamp
    ) {
        DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                .setSensorId(action.sensorId())
                .setType(ActionTypeProto.valueOf(action.type().name()));

        if (action.value() != null) {
            actionBuilder.setValue(action.value());
        }

        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName(action.scenarioName())
                .setAction(actionBuilder)
                .setTimestamp(
                        Timestamp.newBuilder()
                                .setSeconds(timestamp.getEpochSecond())
                                .setNanos(timestamp.getNano())
                )
                .build();

        hubRouterClient
                .withWaitForReady()
                .withDeadlineAfter(60, TimeUnit.SECONDS)
                .handleDeviceAction(request);
    }
}