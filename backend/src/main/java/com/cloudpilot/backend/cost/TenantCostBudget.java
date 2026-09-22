package com.cloudpilot.backend.cost;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "tenant_cost_budgets",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tenant_cost_budget_tenant",
            columnNames = "tenant_id"
        )
    }
)
public class TenantCostBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(
        name = "monthly_budget",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal monthlyBudget;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TenantCostBudget() {
    }

    public TenantCostBudget(
        Long tenantId,
        BigDecimal monthlyBudget,
        String currency,
        Instant updatedAt
    ) {
        this.tenantId = tenantId;
        this.monthlyBudget = monthlyBudget;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }

    public void update(
        BigDecimal monthlyBudget,
        String currency,
        Instant updatedAt
    ) {
        this.monthlyBudget = monthlyBudget;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}