package com.niyotechnologies.claimlens.organization.repository;

import com.niyotechnologies.claimlens.organization.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * All queries are automatically scoped to the current tenant by Hibernate @TenantId, so no
 * method needs a tenantId parameter.
 */
@Repository
public interface RegionRepository
        extends JpaRepository<Region, Long> {

    Optional<Region> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    Optional<Region> findByIdAndIsDeletedFalse(Long id);

    List<Region> findAllByIsDeletedFalse();
}
