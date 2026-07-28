package com.niyotechnologies.claimlens.customer.repository;

import com.niyotechnologies.claimlens.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** All queries are tenant-scoped automatically by Hibernate @TenantId. */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndIsDeletedFalse(Long id);

    boolean existsByCustomerNumberAndIsDeletedFalse(String customerNumber);

    List<Customer> findAllByIsDeletedFalse();

    Page<Customer> findAllByIsDeletedFalse(Pageable pageable);
}
