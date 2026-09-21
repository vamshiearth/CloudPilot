package com.cloudpilot.audit.telemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;

public final class TenantTelemetry {

    public static final String TENANT_ID_KEY = "cloudpilot.tenant.id";

    private TenantTelemetry() {
    }

    public static Long attachCurrentTenantToSpan() {
        String tenantIdValue = Baggage.current().getEntryValue(TENANT_ID_KEY);
        if (tenantIdValue == null || tenantIdValue.isBlank()) {
            return null;
        }

        try {
            long tenantId = Long.parseLong(tenantIdValue);
            Span.current().setAttribute(TENANT_ID_KEY, tenantId);
            return tenantId;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}