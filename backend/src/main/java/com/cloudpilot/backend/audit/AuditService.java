package com.cloudpilot.backend.audit;

import com.cloudpilot.backend.auth.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditServiceClient auditServiceClient;

    public AuditService(AuditServiceClient auditServiceClient) {
        this.auditServiceClient = auditServiceClient;
    }

    public AuditEventPageResponse getEvents(
            AuthenticatedUser authenticatedUser,
            int page,
            int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        return auditServiceClient.getTenantEvents(
                authenticatedUser.tenantId(),
                page,
                size
        );
    }
}
