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

    Optional<Branch> findByCodeAndIsDeletedFalse(
            String code
    );

    boolean existsByCodeAndIsDeletedFalse(
            String code
    );

    List<Branch> findAllByRegionIdAndIsDeletedFalse(
            Long regionId
    );

    /** All branches in the tenant (across regions) — for a flat picker like "home branch". */
    List<Branch> findAllByIsDeletedFalse();

}
