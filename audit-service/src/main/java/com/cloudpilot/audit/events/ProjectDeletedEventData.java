package com.cloudpilot.audit.events;

public record ProjectDeletedEventData(Long projectId, String projectName) {
}
