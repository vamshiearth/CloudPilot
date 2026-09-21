package com.cloudpilot.backend.projects;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    List<Project> findAllByTenant_Id(Long tenantId);

    Optional<Project> findByIdAndTenant_Id(
            Long id,
            Long tenantId
    );

        long countByTenant_Id(Long tenantId);
}
