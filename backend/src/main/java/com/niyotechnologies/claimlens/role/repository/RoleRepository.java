package com.niyotechnologies.claimlens.role.repository;

import com.niyotechnologies.claimlens.role.entity.Role;
import com.niyotechnologies.claimlens.role.enums.RoleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByIdAndIsDeletedFalse(Long id);

    Optional<Role> findByCodeAndIsDeletedFalse(String code);

    List<Role> findAllByIsDeletedFalse();

    boolean existsByCodeAndIsDeletedFalse(String code);

    Optional<Role> findByCodeAndStatusAndIsDeletedFalse(
            String code,
            RoleStatus status
    );
    List<Role> findAllByStatusAndIsDeletedFalse(
            RoleStatus status
    );
}