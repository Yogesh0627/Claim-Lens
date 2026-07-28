package com.niyotechnologies.claimlens.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create a user.
 *
 * <p><b>password</b> is optional and normally omitted: leave it out and the account is created
 * INVITED with a single-use "set your password" link emailed to the person, so an admin never knows
 * anyone else's password. Supplying one activates the account immediately (used by seeds and tests).
 *
 * <p><b>customerId</b> links the login to a policyholder, and is what makes the customer portal work —
 * every portal read is scoped by it. Required when {@code roleCode = CUSTOMER}, and rejected
 * otherwise (staff don't own a customer record). Before this field existed, a CUSTOMER login created
 * through the API had a null customer_id and signed in to a permanently empty portal.
 */
public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String firstName,
        String lastName,
        // Optional at the DTO level: required for staff but auto-generated for a CUSTOMER login (a
        // policyholder is not an employee). The service enforces the staff-only requirement.
        String employeeCode,
        String phone,
        @NotBlank String roleCode,
        @Size(min = 6, max = 100) String password,
        Long customerId,
        // Optional org placement for staff — department, designation, region, home branch and the full
        // set of branches the user works at. Ignored for a CUSTOMER login (a policyholder has none).
        Long departmentId,
        Long designationId,
        Long regionId,
        Long homeBranchId,
        java.util.List<Long> branchIds
) {
}
