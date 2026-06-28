package com.niyotechnologies.claimlens.organization.repository;

import com.niyotechnologies.claimlens.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByIdAndIsDeletedFalse(Long id);

    List<Department> findAllByIsDeletedFalse();
    List<Department> findAllByTenantIdAndIsDeletedFalse(
            Long tenantId
    );

    boolean existsByTenantIdAndCodeAndIsDeletedFalse(Long tenantId, String code);

    boolean existsByTenantIdAndNameAndIsDeletedFalse(Long tenantId, String name);
}