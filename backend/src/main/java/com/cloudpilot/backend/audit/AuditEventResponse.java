package com.cloudpilot.backend.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        Long id,
        UUID eventId,
        Long tenantId,
        Long actorUserId,
        String eventType,
        String resourceType,
        String resourceId,
        String description,
        String eventData,
        Instant occurredAt,
        Instant receivedAt) {
}
