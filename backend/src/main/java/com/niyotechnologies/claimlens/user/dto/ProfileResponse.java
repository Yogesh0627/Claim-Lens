package com.niyotechnologies.claimlens.user.dto;

/**
 * The signed-in user's own profile — richer than {@code MeResponse} (which is just identity +
 * permissions for UI gating). Includes org placement resolved to names. Available to every role.
 */
public record ProfileResponse(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String employeeCode,
        String roleCode,
        String roleName,
        String departmentName,
        String designationName,
        String regionName,
        String homeBranchName,
        /** Where the home branch is — "New Delhi, Delhi" — so the profile shows the actual location. */
        String homeBranchLocation,
        /** Every branch the user works at, home first. */
        java.util.List<String> branchNames,
        String customerNumber
) {
}
