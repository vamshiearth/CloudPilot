package com.cloudpilot.backend.cost;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CostIngestionService {

    private final TenantCostRecordRepository repository;

    public CostIngestionService(TenantCostRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int persist(
        Long tenantId,
        CostSource source,
        List<NormalizedCostRecord> records
    ) {
        int inserted = 0;

        for (NormalizedCostRecord record : records) {
            boolean exists = repository
                .existsByTenantIdAndUsageDateAndServiceCategoryAndSource(
                    tenantId,
                    record.usageDate(),
                    record.serviceCategory(),
                    source
                );

            if (exists) {
                continue;
            }

            repository.save(new TenantCostRecord(
                tenantId,
                record.provider(),
                record.serviceCategory(),
                record.providerServiceName(),
                record.usageDate(),
                record.costAmount(),
                record.currency(),
                source,
                record.recordStatus(),
                Instant.now()
            ));
            inserted++;
        }

        return inserted;
    }
}