package com.niyotechnologies.claimlens.customer.service;

import com.niyotechnologies.claimlens.customer.dto.request.CreateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.request.UpdateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.response.CustomerResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomer(Long customerId);

    List<CustomerResponse> getCustomers();

    CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request);

    void deleteCustomer(Long customerId);
}
