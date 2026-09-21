package com.cloudpilot.backend.rbac;

import com.cloudpilot.backend.tenants.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_role_tenant_name",
                        columnNames = {
                                "tenant_id",
                                "name"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tenant_id",
            nullable = false
    )
    private Tenant tenant;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_role_permission",
                            columnNames = {
                                    "role_id",
                                    "permission_id"
                            }
                    )
            }
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
