package com.cloudpilot.backend.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AuditServiceClient {

    private final RestClient restClient;

    public AuditServiceClient(
            @Value("${cloudpilot.audit-service.base-url}") String auditServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(auditServiceBaseUrl)
                .build();
    }

    public AuditEventPageResponse getTenantEvents(
            Long tenantId,
            int page,
            int size) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/audit/tenants/{tenantId}/events")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build(tenantId))
                    .retrieve()
                    .body(AuditEventPageResponse.class);
        } catch (RestClientException exception) {
            throw new AuditServiceUnavailableException(
                    "Audit Service is currently unavailable",
                    exception
            );
        }
    }
}
