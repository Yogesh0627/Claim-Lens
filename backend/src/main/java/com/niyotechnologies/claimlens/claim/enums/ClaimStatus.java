package com.niyotechnologies.claimlens.claim.enums;

/** The frozen V1 claim lifecycle. */
public enum ClaimStatus {
    DRAFT,
    SUBMITTED,
    AWAITING_ANALYSIS,
    AWAITING_ASSIGNMENT,
    AWAITING_ACCEPTANCE,
    UNDER_INVESTIGATION,
    WAITING_FOR_CUSTOMER,
    APPROVED,
    REJECTED,
    CLOSED,
    REOPENED;

    /** A claim that has reached a final outcome — it no longer blocks a fresh claim for the same loss. */
    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CLOSED;
    }
}
