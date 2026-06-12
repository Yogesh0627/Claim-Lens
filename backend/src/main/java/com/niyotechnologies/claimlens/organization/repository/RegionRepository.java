package com.niyotechnologies.claimlens.organization.repository;

import com.niyotechnologies.claimlens.organization.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegionRepository
        extends JpaRepository<Region, Long> {

    Optional<Region> findByTenantIdAndCode(
            Long tenantId,
            String code
    );

    boolean existsByTenantIdAndCode(
            Long tenantId,
            String code
    );
}