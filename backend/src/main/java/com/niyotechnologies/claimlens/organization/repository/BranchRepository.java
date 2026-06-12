package com.niyotechnologies.claimlens.organization.repository;


import com.niyotechnologies.claimlens.organization.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    Optional<Branch> findByTenantIdAndCode(
            Long tenantId,
            String code
    );

    boolean existsByTenantIdAndCode(
            Long tenantId,
            String code
    );
}