package com.cloudpilot.backend.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionFeatureRepository
        extends JpaRepository<SubscriptionFeature, Long> {

    Optional<SubscriptionFeature> findByName(String name);
}
