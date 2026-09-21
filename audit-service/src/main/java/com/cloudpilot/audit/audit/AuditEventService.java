package com.cloudpilot.audit.audit;

import com.cloudpilot.audit.events.CloudPilotEvent;
import com.cloudpilot.audit.events.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditEventService(
            AuditEventRepository auditEventRepository,
            ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public boolean hasAlreadyProcessed(UUID eventId) {
        return auditEventRepository.existsByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public AuditEventPageResponse getTenantEvents(Long tenantId, int page, int size) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditEvent> result = auditEventRepository
                .findByTenantIdOrderByOccurredAtDesc(tenantId, pageable);

        return new AuditEventPageResponse(
                result.getContent().stream().map(AuditEventResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Transactional
    public AuditEvent recordProjectCreated(
            CloudPilotEvent event,
            ProjectCreatedEventData data) throws JacksonException {
        return saveAuditEvent(
            event,
                "PROJECT",
                data.projectId().toString(),
            "Project \"" + data.projectName() + "\" was created"
        );
        }

        @Transactional
        public AuditEvent recordProjectDeleted(
            CloudPilotEvent event,
            ProjectDeletedEventData data) throws JacksonException {
        return saveAuditEvent(
            event,
            "PROJECT",
            data.projectId().toString(),
            "Project \"" + data.projectName() + "\" was deleted"
        );
        }

        @Transactional
        public AuditEvent recordInvitationCreated(
            CloudPilotEvent event,
            InvitationCreatedEventData data) throws JacksonException {
        return saveAuditEvent(
            event,
            "INVITATION",
            data.invitationId(),
            "Invitation was created for \"" + data.invitedEmail() + "\" with role " + data.role()
        );
        }

        @Transactional
        public AuditEvent recordInvitationAccepted(
            CloudPilotEvent event,
            InvitationAcceptedEventData data) throws JacksonException {
        return saveAuditEvent(
            event,
            "INVITATION",
            data.invitationId().toString(),
            "Invitation for \"" + data.email() + "\" was accepted with role " + data.role()
        );
        }

        @Transactional
        public AuditEvent recordMemberRemoved(
            CloudPilotEvent event,
            MemberRemovedEventData data) throws JacksonException {
        return saveAuditEvent(
            event,
            "MEMBERSHIP",
            data.membershipId().toString(),
            "Member \"" + data.removedUserEmail() + "\" was removed from the tenant"
        );
        }

        @Transactional
        public AuditEvent recordSubscriptionChanged(
            CloudPilotEvent event,
            SubscriptionChangedEventData data) throws JacksonException {
        return saveAuditEvent(
            event,
            "TENANT",
            event.tenantId().toString(),
            "Subscription changed from " + data.previousPlan() + " to " + data.newPlan()
        );
        }

        private AuditEvent saveAuditEvent(
            CloudPilotEvent event,
            String resourceType,
            String resourceId,
            String description) throws JacksonException {
        String eventDataJson = objectMapper.writeValueAsString(event.data());
        AuditEvent auditEvent = new AuditEvent(
            event.eventId(),
            event.tenantId(),
            event.userId(),
            event.eventType(),
            resourceType,
            resourceId,
            description,
            eventDataJson,
            event.occurredAt()
        );
        return auditEventRepository.save(auditEvent);
    }
}
