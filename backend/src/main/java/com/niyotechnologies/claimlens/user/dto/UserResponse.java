package com.niyotechnologies.claimlens.user.dto;

import java.util.List;

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
        String status,
        // Org placement, resolved to names so the directory reads "Claims · Delhi HQ", not raw ids.
        Long departmentId,
        String departmentName,
        Long designationId,
        String designationName,
        Long regionId,
        String regionName,
        Long homeBranchId,
        String homeBranchName,
        // Every branch the user works at (includes the home branch, flagged primary).
        List<BranchRef> branches
) {
    /** A branch the user is assigned to. */
    public record BranchRef(Long id, String name, boolean primary) {
    }
}
