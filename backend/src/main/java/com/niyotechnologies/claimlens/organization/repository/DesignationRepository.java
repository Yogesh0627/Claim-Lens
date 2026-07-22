package com.niyotechnologies.claimlens.organization.repository;

import com.niyotechnologies.claimlens.organization.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {

    Optional<Designation> findByIdAndIsDeletedFalse(Long id);

    List<Designation> findAllByIsDeletedFalse();

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByNameAndIsDeletedFalse(String name);
}
