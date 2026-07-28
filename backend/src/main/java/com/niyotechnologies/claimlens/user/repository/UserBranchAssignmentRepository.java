package com.niyotechnologies.claimlens.user.repository;

import com.niyotechnologies.claimlens.user.entity.UserBranchAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Tenant-scoped by @TenantId on the entity. */
@Repository
public interface UserBranchAssignmentRepository extends JpaRepository<UserBranchAssignment, Long> {

    List<UserBranchAssignment> findAllByUserId(Long userId);

    void deleteByUserId(Long userId);
}
