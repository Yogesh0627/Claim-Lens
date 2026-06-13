package com.niyotechnologies.claimlens.organization.repository;

import com.niyotechnologies.claimlens.organization.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository
        extends JpaRepository<Region, Long> {

    Optional<Region> findByTenantIdAndCodeAndIsDeletedFalse(
            Long tenantId,
            String code
    );

    boolean existsByTenantIdAndCodeAndIsDeletedFalse(
            Long tenantId,
            String code
    );

    Optional<Region>
    findByIdAndIsDeletedFalse(
            Long id
    );

    List<Region> findAllByTenantIdAndIsDeletedFalse(Long tenantId);




}