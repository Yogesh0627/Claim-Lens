package com.niyotechnologies.claimlens.customer.mapper;

import com.niyotechnologies.claimlens.customer.dto.request.CreateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.request.UpdateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.response.CustomerResponse;
import com.niyotechnologies.claimlens.customer.entity.Customer;
import com.niyotechnologies.claimlens.customer.enums.CustomerStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerMapper {

    public Customer toEntity(CreateCustomerRequest request) {
        Customer customer = new Customer();
        // tenant_id and public_id are populated by Hibernate on persist — not set here.
        customer.setCustomerNumber(request.customerNumber().trim());
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(trim(request.lastName()));
        customer.setEmail(trim(request.email()));
        customer.setPhone(trim(request.phone()));
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setNationalId(trim(request.nationalId()));
        customer.setAddress(trim(request.address()));
        customer.setCity(trim(request.city()));
        customer.setState(trim(request.state()));
        customer.setCountry(trim(request.country()));
        customer.setPostalCode(trim(request.postalCode()));
        customer.setStatus(CustomerStatus.ACTIVE);
        return customer;
    }

    public void updateEntity(Customer customer, UpdateCustomerRequest request) {
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(trim(request.lastName()));
        customer.setEmail(trim(request.email()));
        customer.setPhone(trim(request.phone()));
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setNationalId(trim(request.nationalId()));
        customer.setAddress(trim(request.address()));
        customer.setCity(trim(request.city()));
        customer.setState(trim(request.state()));
        customer.setCountry(trim(request.country()));
        customer.setPostalCode(trim(request.postalCode()));
        customer.setStatus(request.status());
    }

    public CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getPublicId(), c.getCustomerNumber(),
                c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone(),
                c.getDateOfBirth(), c.getNationalId(),
                c.getAddress(), c.getCity(), c.getState(), c.getCountry(), c.getPostalCode(),
                c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }

    public List<CustomerResponse> toResponseList(List<Customer> customers) {
        return customers.stream().map(this::toResponse).toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
