package com.cloudpilot.backend.rbac;

import com.cloudpilot.backend.tenants.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class RbacProvisioningService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public RbacProvisioningService(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository) {

        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public void provisionTenant(Tenant tenant) {

        Map<PermissionName, Permission> permissions = createPermissions();

        createOrUpdateRole(
                tenant,
                DefaultRoleName.OWNER,
                "Full organization access",
                EnumSet.allOf(PermissionName.class),
                permissions
        );

        createOrUpdateRole(
                tenant,
                DefaultRoleName.ADMIN,
                "Manage users, roles, projects, tasks, and settings",
                EnumSet.of(
                        PermissionName.PROJECT_CREATE,
                        PermissionName.PROJECT_READ,
                        PermissionName.PROJECT_UPDATE,
                        PermissionName.PROJECT_DELETE,
                        PermissionName.TASK_CREATE,
                        PermissionName.TASK_READ,
                        PermissionName.TASK_UPDATE,
                        PermissionName.TASK_DELETE,
                        PermissionName.USER_READ,
                        PermissionName.USER_INVITE,
                        PermissionName.USER_REMOVE,
                        PermissionName.ROLE_READ,
                        PermissionName.ROLE_ASSIGN,
                        PermissionName.SETTINGS_READ,
                        PermissionName.SETTINGS_UPDATE
                ),
                permissions
        );

        createOrUpdateRole(
                tenant,
                DefaultRoleName.MEMBER,
                "Standard project and task access",
                EnumSet.of(
                        PermissionName.PROJECT_READ,
                        PermissionName.TASK_CREATE,
                        PermissionName.TASK_READ,
                        PermissionName.TASK_UPDATE
                ),
                permissions
        );
    }

    private Map<PermissionName, Permission> createPermissions() {

        Map<PermissionName, Permission> result =
                new EnumMap<>(PermissionName.class);

        for (PermissionName permissionName : PermissionName.values()) {
            Permission permission = permissionRepository
                    .findByName(permissionName.name())
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder()
                                    .name(permissionName.name())
                                    .description(createDescription(permissionName))
                                    .build()
                    ));

            result.put(permissionName, permission);
        }

        return result;
    }

    private void createOrUpdateRole(
            Tenant tenant,
            DefaultRoleName roleName,
            String description,
            Set<PermissionName> permissionNames,
            Map<PermissionName, Permission> permissions) {

        Role role = roleRepository
                .findByTenant_IdAndName(tenant.getId(), roleName.name())
                .orElseGet(() -> Role.builder()
                        .tenant(tenant)
                        .name(roleName.name())
                        .description(description)
                        .build());

        role.setDescription(description);

        Set<Permission> rolePermissions = new HashSet<>();
        for (PermissionName permissionName : permissionNames) {
            rolePermissions.add(permissions.get(permissionName));
        }

        role.setPermissions(rolePermissions);
        roleRepository.save(role);
    }

    private String createDescription(PermissionName permissionName) {
        return permissionName.name().toLowerCase().replace("_", " ");
    }
}