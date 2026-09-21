package com.cloudpilot.audit.audit;

import com.cloudpilot.audit.events.CloudPilotEventType;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        Long id,
        UUID eventId,
        Long tenantId,
        Long actorUserId,
        CloudPilotEventType eventType,
        String resourceType,
        String resourceId,
        String description,
        String eventData,
        Instant occurredAt,
        Instant receivedAt) {

    public static AuditEventResponse from(AuditEvent auditEvent) {
        return new AuditEventResponse(
                auditEvent.getId(),
                auditEvent.getEventId(),
                auditEvent.getTenantId(),
                auditEvent.getActorUserId(),
                auditEvent.getEventType(),
                auditEvent.getResourceType(),
                auditEvent.getResourceId(),
                auditEvent.getDescription(),
                auditEvent.getEventData(),
                auditEvent.getOccurredAt(),
                auditEvent.getReceivedAt()
        );
    }
}
