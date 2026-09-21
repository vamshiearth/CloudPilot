package com.cloudpilot.backend.events;

import java.time.Instant;
import java.util.UUID;

public record CloudPilotEvent(

        UUID eventId,

        CloudPilotEventType eventType,

        Long tenantId,

        Long userId,

        Instant occurredAt,

        Object data

) {

    public CloudPilotEvent {

        if (eventId == null) {
            throw new IllegalArgumentException(
                    "eventId cannot be null"
            );
        }

        if (eventType == null) {
            throw new IllegalArgumentException(
                    "eventType cannot be null"
            );
        }

        if (tenantId == null) {
            throw new IllegalArgumentException(
                    "tenantId cannot be null"
            );
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "occurredAt cannot be null"
            );
        }

    }

    public static CloudPilotEvent create(
            CloudPilotEventType eventType,
            Long tenantId,
            Long userId,
            Object data) {

        return new CloudPilotEvent(
                UUID.randomUUID(),
                eventType,
                tenantId,
                userId,
                Instant.now(),
                data
        );
    }
}