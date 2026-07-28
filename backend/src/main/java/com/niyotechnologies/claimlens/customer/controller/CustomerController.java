package com.niyotechnologies.claimlens.customer.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.common.response.PagedResponse;
import com.niyotechnologies.claimlens.customer.dto.request.CreateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.request.UpdateCustomerRequest;
import com.niyotechnologies.claimlens.customer.dto.response.CustomerResponse;
import com.niyotechnologies.claimlens.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/customers")
@RequiredArgsConstructor
public class CustomerController {

    @Autowired
    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.success(customerService.createCustomer(request));
    }

    @GetMapping("/{customerId}")
    public ApiResponse<CustomerResponse> getCustomer(@PathVariable Long customerId) {
        return ApiResponse.success(customerService.getCustomer(customerId));
    }

    @GetMapping
    public ApiResponse<PagedResponse<CustomerResponse>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(customerService.getCustomers(page, size));
    }

    /** Unpaged — for customer pickers (new claim, new policy, customer-role user). */
    @GetMapping("/options")
    public ApiResponse<List<CustomerResponse>> getCustomerOptions() {
        return ApiResponse.success(customerService.getCustomerOptions());
    }

    @PutMapping("/{customerId}")
    public ApiResponse<CustomerResponse> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ApiResponse.success(customerService.updateCustomer(customerId, request));
    }

    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long customerId) {
        customerService.deleteCustomer(customerId);
    }
}
