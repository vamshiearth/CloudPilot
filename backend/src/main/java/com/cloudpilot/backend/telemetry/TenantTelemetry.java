package com.cloudpilot.backend.telemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

public final class TenantTelemetry {

    public static final String TENANT_ID_KEY = "cloudpilot.tenant.id";

    private TenantTelemetry() {
    }

    public static Scope bindTenant(long tenantId) {
        Span.current().setAttribute(TENANT_ID_KEY, tenantId);

        Baggage baggage = Baggage.current()
                .toBuilder()
                .put(TENANT_ID_KEY, Long.toString(tenantId))
                .build();

        return baggage.makeCurrent();
    }
}