package com.cloudpilot.backend.cost;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

@Component
@Profile("dev")
public class SimulatedCostDataSeeder implements CommandLineRunner {

    private static final String CURRENCY = "USD";

    private final TenantCostRecordRepository costRecordRepository;

    public SimulatedCostDataSeeder(
        TenantCostRecordRepository costRecordRepository
    ) {
        this.costRecordRepository = costRecordRepository;
    }

    @Override
    public void run(String... args) {
        seedTenant(1L, new BigDecimal("1.00"));
        seedTenant(2L, new BigDecimal("1.35"));
    }

    private void seedTenant(Long tenantId, BigDecimal multiplier) {
        LocalDate today = LocalDate.now();

        for (int daysAgo = 30; daysAgo >= 0; daysAgo--) {
            LocalDate usageDate = today.minusDays(daysAgo);
            BigDecimal dayFactor = BigDecimal.valueOf(31L - daysAgo)
                .divide(BigDecimal.valueOf(31L), 6, RoundingMode.HALF_UP);

            seedRecord(
                tenantId,
                usageDate,
                CloudServiceCategory.KUBERNETES,
                "Amazon EKS",
                new BigDecimal("4.50").multiply(multiplier).add(dayFactor)
            );
            seedRecord(
                tenantId,
                usageDate,
                CloudServiceCategory.COMPUTE,
                "Amazon EC2",
                new BigDecimal("3.20").multiply(multiplier)
                    .add(dayFactor.multiply(new BigDecimal("0.60")))
            );
            seedRecord(
                tenantId,
                usageDate,
                CloudServiceCategory.STORAGE,
                "Amazon EBS",
                new BigDecimal("1.25").multiply(multiplier)
            );
            seedRecord(
                tenantId,
                usageDate,
                CloudServiceCategory.CONTAINER_REGISTRY,
                "Amazon ECR",
                new BigDecimal("0.35").multiply(multiplier)
            );
            seedRecord(
                tenantId,
                usageDate,
                CloudServiceCategory.OBSERVABILITY,
                "CloudWatch",
                new BigDecimal("0.80").multiply(multiplier)
                    .add(dayFactor.multiply(new BigDecimal("0.20")))
            );
        }
    }

    private void seedRecord(
        Long tenantId,
        LocalDate usageDate,
        CloudServiceCategory category,
        String providerServiceName,
        BigDecimal amount
    ) {
        boolean exists = costRecordRepository
            .existsByTenantIdAndUsageDateAndServiceCategoryAndSource(
                tenantId,
                usageDate,
                category,
                CostSource.SIMULATED
            );

        if (exists) {
            return;
        }

        TenantCostRecord record = new TenantCostRecord(
            tenantId,
            CloudProvider.AWS,
            category,
            providerServiceName,
            usageDate,
            amount.setScale(6, RoundingMode.HALF_UP),
            CURRENCY,
            CostSource.SIMULATED,
            CostRecordStatus.FINAL,
            Instant.now()
        );

        costRecordRepository.save(record);
    }
}