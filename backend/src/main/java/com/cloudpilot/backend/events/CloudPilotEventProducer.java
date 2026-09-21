package com.cloudpilot.backend.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

@Service
public class CloudPilotEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CloudPilotEventProducer.class
            );

    private final KafkaTemplate<String, String>
            kafkaTemplate;

    private final ObjectMapper
            objectMapper;

    private final String topic;

    public CloudPilotEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${cloudpilot.kafka.topic}")
            String topic) {

        this.kafkaTemplate =
                kafkaTemplate;

        this.objectMapper =
                objectMapper;

        this.topic =
                topic;
    }

    public CompletableFuture<SendResult<String, String>>
    publish(CloudPilotEvent event) {

        final String json;

        try {

            json =
                    objectMapper
                            .writeValueAsString(
                                    event
                            );

        } catch (JacksonException exception) {

            log.error(
                    "Failed to serialize CloudPilot event. eventId={}, eventType={}, tenantId={}",
                    event.eventId(),
                    event.eventType(),
                    event.tenantId(),
                    exception
            );

            return CompletableFuture.failedFuture(exception);
        }

        String key =
                event.tenantId()
                        .toString();

        try {

            CompletableFuture<SendResult<String, String>>
                    future =
                    kafkaTemplate.send(
                            topic,
                            key,
                            json
                    );

            future.whenComplete(
                    (result, exception) -> {

                        if (exception != null) {

                            log.error(
                                    "Kafka publish failed. eventId={}, eventType={}, tenantId={}",
                                    event.eventId(),
                                    event.eventType(),
                                    event.tenantId(),
                                    exception
                            );

                            return;
                        }

                        log.debug(
                                "Kafka event published. eventId={}, eventType={}, tenantId={}, partition={}, offset={}",
                                event.eventId(),
                                event.eventType(),
                                event.tenantId(),
                                result.getRecordMetadata()
                                        .partition(),
                                result.getRecordMetadata()
                                        .offset()
                        );
                    }
            );

            return future;

        } catch (RuntimeException exception) {

            log.error(
                    "Kafka unavailable while publishing event. eventId={}, eventType={}, tenantId={}. Business operation will continue.",
                    event.eventId(),
                    event.eventType(),
                    event.tenantId(),
                    exception
            );

            return CompletableFuture.failedFuture(exception);
        }
    }
}
