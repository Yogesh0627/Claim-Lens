package com.niyotechnologies.claimlens.organization.repository;

import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceCompanyRepository
        extends JpaRepository<InsuranceCompany, Long> {

    Optional<InsuranceCompany> findByCode(String code);

    Optional<InsuranceCompany> findByTenantKey(String tenantKey);

    Optional<InsuranceCompany>
    findByIdAndIsDeletedFalse(
            Long id
    );

    boolean existsByCode(String code);

    boolean existsByTenantKey(String tenantKey);
}