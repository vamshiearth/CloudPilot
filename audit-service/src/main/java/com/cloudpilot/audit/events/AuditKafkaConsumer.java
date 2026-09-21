package com.cloudpilot.audit.events;

import com.cloudpilot.audit.audit.AuditEvent;
import com.cloudpilot.audit.audit.AuditEventService;
import com.cloudpilot.audit.telemetry.TenantTelemetry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AuditKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final AuditEventService auditEventService;

    public AuditKafkaConsumer(ObjectMapper objectMapper, AuditEventService auditEventService) {
        this.objectMapper = objectMapper;
        this.auditEventService = auditEventService;
    }

    @KafkaListener(
            topics = "${cloudpilot.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        Long propagatedTenantId = TenantTelemetry.attachCurrentTenantToSpan();
        log.info("Received propagated tenant context tenantId={}", propagatedTenantId);

        try {
            CloudPilotEvent event = objectMapper.readValue(record.value(), CloudPilotEvent.class);
            String expectedKey = event.tenantId().toString();

            if (!expectedKey.equals(record.key())) {
                log.warn(
                        "Ignoring Kafka event because tenant key does not match event tenantId. eventId={}, tenantId={}, kafkaKey={}",
                        event.eventId(),
                        event.tenantId(),
                        record.key()
                );
                return;
            }

            if (auditEventService.hasAlreadyProcessed(event.eventId())) {
                log.info(
                        "Duplicate Kafka event ignored. eventId={}, eventType={}, tenantId={}, partition={}, offset={}",
                        event.eventId(),
                        event.eventType(),
                        event.tenantId(),
                        record.partition(),
                        record.offset()
                );
                return;
            }

            log.info(
                    "Audit event received. eventId={}, eventType={}, tenantId={}, userId={}, key={}, partition={}, offset={}",
                    event.eventId(),
                    event.eventType(),
                    event.tenantId(),
                    event.userId(),
                    record.key(),
                    record.partition(),
                    record.offset()
            );

            handleEvent(event);
        } catch (JacksonException | IllegalArgumentException exception) {
            log.warn(
                    "Audit Service ignored invalid Kafka event. key={}, partition={}, offset={}",
                    record.key(),
                    record.partition(),
                    record.offset(),
                    exception
            );
        }
    }

    private void handleEvent(CloudPilotEvent event) throws JacksonException {
        switch (event.eventType()) {
            case PROJECT_CREATED -> handleProjectCreated(event);
            case PROJECT_DELETED -> handleProjectDeleted(event);
            case INVITATION_CREATED -> handleInvitationCreated(event);
            case INVITATION_ACCEPTED -> handleInvitationAccepted(event);
            case MEMBER_REMOVED -> handleMemberRemoved(event);
            case SUBSCRIPTION_CHANGED -> handleSubscriptionChanged(event);
        }
    }

    private void handleProjectCreated(CloudPilotEvent event) throws JacksonException {
        ProjectCreatedEventData data = objectMapper.treeToValue(event.data(), ProjectCreatedEventData.class);
        AuditEvent savedAuditEvent = auditEventService.recordProjectCreated(event, data);
        log.info(
            "PROJECT_CREATED audit persisted. auditId={}, eventId={}, tenantId={}, projectId={}, projectName={}",
            savedAuditEvent.getId(), savedAuditEvent.getEventId(), savedAuditEvent.getTenantId(),
            data.projectId(), data.projectName()
        );
    }

    private void handleProjectDeleted(CloudPilotEvent event) throws JacksonException {
        ProjectDeletedEventData data = objectMapper.treeToValue(event.data(), ProjectDeletedEventData.class);
        AuditEvent savedAuditEvent = auditEventService.recordProjectDeleted(event, data);
        log.info(
            "PROJECT_DELETED audit persisted. auditId={}, eventId={}, tenantId={}, projectId={}",
            savedAuditEvent.getId(), savedAuditEvent.getEventId(), savedAuditEvent.getTenantId(), data.projectId()
        );
    }

    private void handleInvitationCreated(CloudPilotEvent event) throws JacksonException {
        InvitationCreatedEventData data = objectMapper.treeToValue(event.data(), InvitationCreatedEventData.class);
        AuditEvent savedAuditEvent = auditEventService.recordInvitationCreated(event, data);
        log.info(
            "INVITATION_CREATED audit persisted. auditId={}, eventId={}, tenantId={}, invitationId={}, email={}",
            savedAuditEvent.getId(), savedAuditEvent.getEventId(), savedAuditEvent.getTenantId(),
            data.invitationId(), data.invitedEmail()
        );
    }

    private void handleInvitationAccepted(CloudPilotEvent event) throws JacksonException {
        InvitationAcceptedEventData data = objectMapper.treeToValue(event.data(), InvitationAcceptedEventData.class);
        AuditEvent savedAuditEvent = auditEventService.recordInvitationAccepted(event, data);
        log.info(
            "INVITATION_ACCEPTED audit persisted. auditId={}, eventId={}, tenantId={}, invitationId={}, membershipId={}",
            savedAuditEvent.getId(), savedAuditEvent.getEventId(), savedAuditEvent.getTenantId(),
            data.invitationId(), data.membershipId()
        );
    }

    private void handleMemberRemoved(CloudPilotEvent event) throws JacksonException {
        MemberRemovedEventData data = objectMapper.treeToValue(event.data(), MemberRemovedEventData.class);
        AuditEvent savedAuditEvent = auditEventService.recordMemberRemoved(event, data);
        log.info(
            "MEMBER_REMOVED audit persisted. auditId={}, eventId={}, tenantId={}, membershipId={}, removedUserId={}",
            savedAuditEvent.getId(), savedAuditEvent.getEventId(), savedAuditEvent.getTenantId(),
            data.membershipId(), data.removedUserId()
        );
    }

    private void handleSubscriptionChanged(CloudPilotEvent event) throws JacksonException {
        SubscriptionChangedEventData data = objectMapper.treeToValue(event.data(), SubscriptionChangedEventData.class);
        AuditEvent savedAuditEvent = auditEventService.recordSubscriptionChanged(event, data);
        log.info(
            "SUBSCRIPTION_CHANGED audit persisted. auditId={}, eventId={}, tenantId={}, previousPlan={}, newPlan={}",
            savedAuditEvent.getId(), savedAuditEvent.getEventId(), savedAuditEvent.getTenantId(),
            data.previousPlan(), data.newPlan()
        );
    }
}
