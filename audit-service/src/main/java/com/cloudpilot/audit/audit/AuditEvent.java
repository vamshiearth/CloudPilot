package com.cloudpilot.audit.audit;

import com.cloudpilot.audit.events.CloudPilotEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "audit_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_audit_events_event_id",
                columnNames = "event_id"
        ),
        indexes = {
                @Index(name = "idx_audit_events_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_audit_events_tenant_occurred", columnList = "tenant_id, occurred_at")
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private CloudPilotEventType eventType;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected AuditEvent() {
    }

    public AuditEvent(
            UUID eventId,
            Long tenantId,
            Long actorUserId,
            CloudPilotEventType eventType,
            String resourceType,
            String resourceId,
            String description,
            String eventData,
            Instant occurredAt) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.description = description;
        this.eventData = eventData;
        this.occurredAt = occurredAt;
        this.receivedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public CloudPilotEventType getEventType() {
        return eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getDescription() {
        return description;
    }

    public String getEventData() {
        return eventData;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
