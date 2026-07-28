package com.niyotechnologies.claimlens.customer.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.common.response.PagedResponse;
import com.niyotechnologies.claimlens.common.util.PageRequests;
import org.springframework.data.domain.Sort;
import com.niyotechnologies.claimlens.customer.dto.request.CreateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.request.UpdateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.response.CustomerResponse;
import com.niyotechnologies.claimlens.customer.entity.Customer;
import com.niyotechnologies.claimlens.customer.mapper.CustomerMapper;
import com.niyotechnologies.claimlens.customer.repository.CustomerRepository;
import com.niyotechnologies.claimlens.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Tenant scoping is enforced by Hibernate @TenantId; this service never takes a tenant argument. */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    private Customer getCustomerOrThrow(Long customerId) {
        return customerRepository.findByIdAndIsDeletedFalse(customerId)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByCustomerNumberAndIsDeletedFalse(request.customerNumber())) {
            throw new BusinessException("CUSTOMER_NUMBER_ALREADY_EXISTS",
                    "Customer number already exists");
        }
        Customer customer = customerMapper.toEntity(request);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public CustomerResponse getCustomer(Long customerId) {
        return customerMapper.toResponse(getCustomerOrThrow(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public PagedResponse<CustomerResponse> getCustomers(int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return PagedResponse.from(
                customerRepository.findAllByIsDeletedFalse(pageable),
                customerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public List<CustomerResponse> getCustomerOptions() {
        return customerMapper.toResponseList(customerRepository.findAllByIsDeletedFalse());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request) {
        Customer customer = getCustomerOrThrow(customerId);
        customerMapper.updateEntity(customer, request);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public void deleteCustomer(Long customerId) {
        Customer customer = getCustomerOrThrow(customerId);
        customer.setIsDeleted(true);
        customer.setDeletedAt(Instant.now());
        customerRepository.save(customer);
    }
}
