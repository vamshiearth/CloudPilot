package com.cloudpilot.backend.subscriptions;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "subscription_plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_plan_name",
                        columnNames = "name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String displayName;

    private String description;

    @Column(nullable = false)
    private boolean active;

        @Column(name = "max_members")
        private Integer maxMembers;

        @Column(name = "max_projects")
        private Integer maxProjects;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "subscription_plan_features",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_plan_feature",
                    columnNames = {"plan_id", "feature_id"}
            )
    )
    @Builder.Default
    private Set<SubscriptionFeature> features = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
