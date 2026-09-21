package com.cloudpilot.audit.audit;

import java.util.List;

public record AuditEventPageResponse(
        List<AuditEventResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
