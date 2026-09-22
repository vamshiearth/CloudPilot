package com.cloudpilot.backend.cost;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "tenant_cost_records",
    indexes = {
        @Index(
            name = "idx_tenant_cost_records_tenant_date",
            columnList = "tenant_id, usage_date"
        ),
        @Index(
            name = "idx_tenant_cost_records_tenant_service",
            columnList = "tenant_id, service_category"
        )
    }
)
public class TenantCostRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private CloudProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", nullable = false, length = 64)
    private CloudServiceCategory serviceCategory;

    @Column(name = "provider_service_name", length = 128)
    private String providerServiceName;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(
        name = "cost_amount",
        nullable = false,
        precision = 19,
        scale = 6
    )
    private BigDecimal costAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private CostSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 16)
    private CostRecordStatus recordStatus;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    protected TenantCostRecord() {
    }

    public TenantCostRecord(
        Long tenantId,
        CloudProvider provider,
        CloudServiceCategory serviceCategory,
        String providerServiceName,
        LocalDate usageDate,
        BigDecimal costAmount,
        String currency,
        CostSource source,
        CostRecordStatus recordStatus,
        Instant observedAt
    ) {
        this.tenantId = tenantId;
        this.provider = provider;
        this.serviceCategory = serviceCategory;
        this.providerServiceName = providerServiceName;
        this.usageDate = usageDate;
        this.costAmount = costAmount;
        this.currency = currency;
        this.source = source;
        this.recordStatus = recordStatus;
        this.observedAt = observedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public CloudProvider getProvider() {
        return provider;
    }

    public CloudServiceCategory getServiceCategory() {
        return serviceCategory;
    }

    public String getProviderServiceName() {
        return providerServiceName;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public CostSource getSource() {
        return source;
    }

    public CostRecordStatus getRecordStatus() {
        return recordStatus;
    }

    public Instant getObservedAt() {
        return observedAt;
    }
}