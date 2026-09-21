package com.cloudpilot.backend.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByName(
            String name
    );

    boolean existsByName(
            String name
    );

        List<SubscriptionPlan> findAllByActiveTrueOrderByIdAsc();
}
