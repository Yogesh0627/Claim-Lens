package com.niyotechnologies.claimlens.auth.enums;

/**
 * Why a password-setting token was issued. Both use the same single-use, hashed-token machinery;
 * they differ in lifetime and in what redeeming them means.
 */
public enum InvitationPurpose {

    /** Admin created the account without a password — redeeming it activates the account. */
    INVITE,

    /** Self-service "forgot password" — redeeming it replaces the password on an active account. */
    RESET
}
