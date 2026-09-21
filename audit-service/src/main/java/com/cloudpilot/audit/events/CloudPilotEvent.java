package com.cloudpilot.audit.events;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record CloudPilotEvent(
        UUID eventId,
        CloudPilotEventType eventType,
        Long tenantId,
        Long userId,
        Instant occurredAt,
        JsonNode data
) {

    public CloudPilotEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId cannot be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt cannot be null");
        }
    }
}
