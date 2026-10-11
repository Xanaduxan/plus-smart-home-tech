package ru.yandex.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.handler.HubEventHandler;
import ru.yandex.practicum.collector.handler.SensorEventHandler;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class EventController
        extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    private final Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;

    public EventController(
            Set<SensorEventHandler> sensorEventHandlers,
            Set<HubEventHandler> hubEventHandlers
    ) {
        this.sensorEventHandlers = sensorEventHandlers.stream()
                .collect(Collectors.toMap(
                        SensorEventHandler::getMessageType,
                        Function.identity()
                ));

        this.hubEventHandlers = hubEventHandlers.stream()
                .collect(Collectors.toMap(
                        HubEventHandler::getMessageType,
                        Function.identity()
                ));
    }

    /**
     * Метод для обработки событий от датчиков.
     * Вызывается при получении нового события от gRPC-клиента.
     *
     * @param request           Событие от датчика
     * @param responseObserver  Ответ для клиента
     */
    @Override
    public void collectSensorEvent(
            SensorEventProto request,
            StreamObserver<Empty> responseObserver
    ) {
        try {
            if (sensorEventHandlers.containsKey(request.getPayloadCase())) {
                sensorEventHandlers.get(request.getPayloadCase()).handle(request);
            } else {
                throw new IllegalArgumentException(
                        "Не могу найти обработчик для события "
                                + request.getPayloadCase()
                );
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(
                    "Ошибка обработки события датчика: id={}, hubId={}, payload={}",
                    request.getId(),
                    request.getHubId(),
                    request.getPayloadCase(),
                    e
            );

            responseObserver.onError(
                    Status.fromThrowable(e)
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    /**
     * Метод для обработки событий от хабов.
     *
     * @param request           Событие от хаба
     * @param responseObserver  Ответ для клиента
     */
    @Override
    public void collectHubEvent(
            HubEventProto request,
            StreamObserver<Empty> responseObserver
    ) {
        try {
            if (hubEventHandlers.containsKey(request.getPayloadCase())) {
                hubEventHandlers.get(request.getPayloadCase()).handle(request);
            } else {
                throw new IllegalArgumentException(
                        "Не могу найти обработчик для события "
                                + request.getPayloadCase()
                );
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    new StatusRuntimeException(Status.fromThrowable(e))
            );
        }
    }
}
