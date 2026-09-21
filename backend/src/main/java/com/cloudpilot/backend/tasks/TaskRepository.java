package com.cloudpilot.backend.tasks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository
        extends JpaRepository<Task, Long> {

        List<Task> findAllByProject_IdAndTenant_Id(
            Long projectId,
            Long tenantId
        );

        Optional<Task> findByIdAndTenant_Id(
            Long id,
            Long tenantId
        );

    boolean existsByProjectId(Long projectId);
}
