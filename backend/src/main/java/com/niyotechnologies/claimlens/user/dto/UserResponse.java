package com.niyotechnologies.claimlens.user.dto;

/** A back-office user, for directory listings (e.g. picking an investigator to assign a claim). */
public record UserResponse(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String phone,
        Long roleId,
        String roleCode,
        String status
) {
}
