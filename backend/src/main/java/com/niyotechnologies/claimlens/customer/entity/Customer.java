package com.niyotechnologies.claimlens.customer.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.customer.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The policyholder. Tenant-scoped via @TenantId. public_id is Hibernate-generated (UuidGenerator)
 * so it is never null on insert; the DB also has a gen_random_uuid() default for raw inserts.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
public class Customer extends TenantAwareEntity {

    @UuidGenerator
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "customer_number", nullable = false, length = 50)
    private String customerNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "national_id", length = 64)
    private String nationalId;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status;
}
