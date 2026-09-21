package com.cloudpilot.backend.tenants;

import com.cloudpilot.backend.rbac.Role;
import com.cloudpilot.backend.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenant_user",
                        columnNames = {
                                "tenant_id",
                                "user_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tenant_id",
            nullable = false
    )
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "role_id")
        private Role role;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(
            name = "joined_at",
            updatable = false
    )
    private LocalDateTime joinedAt;
}
