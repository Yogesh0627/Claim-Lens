package com.niyotechnologies.claimlens.organization.repository;


import com.niyotechnologies.claimlens.organization.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    Optional<Branch> findByIdAndIsDeletedFalse(
            Long id
    );

    Optional<Branch> findByTenantIdAndCodeAndIsDeletedFalse(
            Long tenantId,
            String code
    );

    boolean existsByTenantIdAndCodeAndIsDeletedFalse(
            Long tenantId,
            String code
    );

    List<Branch> findAllByRegionIdAndIsDeletedFalse(
            Long regionId
    );


}