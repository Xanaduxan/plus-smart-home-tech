package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.kafka.KafkaProperties;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final Producer<String, SpecificRecordBase> producer;
    private final KafkaProperties kafkaProperties;

    @Override
    public void collectSensorEvent(SensorEvent event) {
        send(
                kafkaProperties.getTopics().getSensors(),
                event.getHubId(),
                sensorEventMapper.toAvro(event)
        );
    }

    @Override
    public void collectHubEvent(HubEvent event) {
        send(
                kafkaProperties.getTopics().getHubs(),
                event.getHubId(),
                hubEventMapper.toAvro(event)
        );
    }

    private void send(
            String topic,
            String hubId,
            SpecificRecordBase event
    ) {
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(topic, hubId, event);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error(
                        "Ошибка отправки события в Kafka: topic={}, hubId={}",
                        topic,
                        hubId,
                        exception
                );
            }
        });
    }
}
