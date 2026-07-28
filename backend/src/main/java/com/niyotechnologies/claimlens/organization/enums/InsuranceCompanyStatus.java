package com.niyotechnologies.claimlens.organization.enums;

public enum InsuranceCompanyStatus {
    ONBOARDING,
    ACTIVE,
    SUSPENDED,
    // A retired tenant kept for records — distinct from a soft delete (is_deleted). Archived is a
    // status the platform admin can move a tenant into and back out of; deletion is removal.
    ARCHIVED
}
