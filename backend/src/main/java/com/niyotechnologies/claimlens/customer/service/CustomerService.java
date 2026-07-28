package com.niyotechnologies.claimlens.customer.service;

import com.niyotechnologies.claimlens.customer.dto.request.CreateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.request.UpdateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.response.CustomerResponse;
import com.niyotechnologies.claimlens.common.response.PagedResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomer(Long customerId);

    /** Paged directory listing for the Customers screen. */
    PagedResponse<CustomerResponse> getCustomers(int page, int size);

    /** All customers (unpaged) — for pickers/dropdowns that must show every option. */
    List<CustomerResponse> getCustomerOptions();

    CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request);

    void deleteCustomer(Long customerId);
}
