package com.cloudpilot.backend.cost;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(
    name = "cloudpilot.cost.aws.enabled",
    havingValue = "true"
)
public class AwsCostExplorerProvider implements CostDataProvider {

    @Override
    public CostSource source() {
        return CostSource.AWS_COST_EXPLORER;
    }

    @Override
    public List<NormalizedCostRecord> fetchDailyCosts(
        Long tenantId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        throw new UnsupportedOperationException(
            "AWS Cost Explorer integration is not enabled yet"
        );
    }
}