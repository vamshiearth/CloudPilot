package com.cloudpilot.backend.events;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class CloudPilotEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CloudPilotEventConsumer.class
            );

    private final ObjectMapper objectMapper;

    public CloudPilotEventConsumer(
            ObjectMapper objectMapper) {

        this.objectMapper =
                objectMapper;
    }

    @KafkaListener(
            topics = "${cloudpilot.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
                        ConsumerRecord<String, String> record) {

                String message = record.value();

                log.info(
                                "Kafka record received. key={}, partition={}, offset={}",
                                record.key(),
                                record.partition(),
                                record.offset()
                );

        try {

            CloudPilotEvent event =
                    objectMapper.readValue(
                            message,
                            CloudPilotEvent.class
                    );

            handleEvent(event);

        } catch (JacksonException | IllegalArgumentException exception) {

            log.warn(
                    "Ignoring invalid CloudPilot Kafka message: {}",
                    message
            );
        }
    }

    private void handleEvent(
            CloudPilotEvent event) {

        switch (event.eventType()) {

            case PROJECT_CREATED ->
                    handleProjectCreated(
                            event
                    );

            case PROJECT_DELETED ->
                    handleProjectDeleted(
                            event
                    );

            case INVITATION_CREATED ->
                    handleInvitationCreated(
                            event
                    );

            case INVITATION_ACCEPTED ->
                    handleInvitationAccepted(
                            event
                    );

            case MEMBER_REMOVED ->
                    handleMemberRemoved(
                            event
                    );

            case SUBSCRIPTION_CHANGED ->
                    handleSubscriptionChanged(
                            event
                    );

            default ->
                    log.debug(
                            "Kafka event received but no handler exists yet. eventId={}, eventType={}, tenantId={}",
                            event.eventId(),
                            event.eventType(),
                            event.tenantId()
                    );
        }
    }

    private void handleProjectCreated(
            CloudPilotEvent event) {

        ProjectCreatedEventData data =
                objectMapper.convertValue(
                        event.data(),
                        ProjectCreatedEventData.class
                );

        log.info(
                "PROJECT_CREATED event received. eventId={}, tenantId={}, userId={}, projectId={}, projectName={}, occurredAt={}",
                event.eventId(),
                event.tenantId(),
                event.userId(),
                data.projectId(),
                data.projectName(),
                event.occurredAt()
        );
    }

    private void handleProjectDeleted(
            CloudPilotEvent event) {

        ProjectDeletedEventData data =
                objectMapper.convertValue(
                        event.data(),
                        ProjectDeletedEventData.class
                );

        log.info(
                "PROJECT_DELETED event received. eventId={}, tenantId={}, userId={}, projectId={}, projectName={}, occurredAt={}",
                event.eventId(),
                event.tenantId(),
                event.userId(),
                data.projectId(),
                data.projectName(),
                event.occurredAt()
        );
    }

    private void handleInvitationCreated(
            CloudPilotEvent event) {

        InvitationCreatedEventData data =
                objectMapper.convertValue(
                        event.data(),
                        InvitationCreatedEventData.class
                );

        log.info(
                "INVITATION_CREATED event received. eventId={}, tenantId={}, userId={}, invitationId={}, email={}, role={}, occurredAt={}",
                event.eventId(),
                event.tenantId(),
                event.userId(),
                data.invitationId(),
                data.invitedEmail(),
                data.role(),
                event.occurredAt()
        );
    }

    private void handleInvitationAccepted(
            CloudPilotEvent event) {

        InvitationAcceptedEventData data =
                objectMapper.convertValue(
                        event.data(),
                        InvitationAcceptedEventData.class
                );

        log.info(
                "INVITATION_ACCEPTED event received. eventId={}, tenantId={}, userId={}, invitationId={}, membershipId={}, acceptedUserId={}, email={}, role={}, occurredAt={}",
                event.eventId(),
                event.tenantId(),
                event.userId(),
                data.invitationId(),
                data.membershipId(),
                data.acceptedUserId(),
                data.email(),
                data.role(),
                event.occurredAt()
        );
    }

    private void handleMemberRemoved(
            CloudPilotEvent event) {

        MemberRemovedEventData data =
                objectMapper.convertValue(
                        event.data(),
                        MemberRemovedEventData.class
                );

        log.info(
                "MEMBER_REMOVED event received. eventId={}, tenantId={}, userId={}, membershipId={}, removedUserId={}, removedUserEmail={}, role={}, occurredAt={}",
                event.eventId(),
                event.tenantId(),
                event.userId(),
                data.membershipId(),
                data.removedUserId(),
                data.removedUserEmail(),
                data.role(),
                event.occurredAt()
        );
    }

    private void handleSubscriptionChanged(
            CloudPilotEvent event) {

        SubscriptionChangedEventData data =
                objectMapper.convertValue(
                        event.data(),
                        SubscriptionChangedEventData.class
                );

        log.info(
                "SUBSCRIPTION_CHANGED event received. eventId={}, tenantId={}, userId={}, previousPlan={}, newPlan={}, occurredAt={}",
                event.eventId(),
                event.tenantId(),
                event.userId(),
                data.previousPlan(),
                data.newPlan(),
                event.occurredAt()
        );
    }
}