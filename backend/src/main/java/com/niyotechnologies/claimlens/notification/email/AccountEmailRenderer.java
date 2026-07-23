package com.niyotechnologies.claimlens.notification.email;

import com.niyotechnologies.claimlens.auth.enums.InvitationPurpose;
import org.springframework.stereotype.Component;

/**
 * Renders the two account emails — the invitation sent when an admin creates someone's account, and
 * the self-service password reset. Branded to match the claim emails; plain functions, no I/O, so
 * they're unit-testable.
 */
@Component
public class AccountEmailRenderer {

    public String subject(InvitationPurpose purpose) {
        return purpose == InvitationPurpose.RESET
                ? "Reset your ClaimLens password"
                : "You've been invited to ClaimLens";
    }

    public String html(String firstName, InvitationPurpose purpose, String link) {
        boolean reset = purpose == InvitationPurpose.RESET;
        String headline = reset ? "Reset your password" : "Welcome to ClaimLens";
        String lead = reset
                ? "We received a request to reset your ClaimLens password. Choose a new one below."
                : "An account has been created for you on ClaimLens. Set a password to get started.";

        String content = EmailLayout.paragraph("Hi " + safeName(firstName) + ",")
                + EmailLayout.paragraph(lead)
                + EmailLayout.button(reset ? "Choose a new password" : "Set your password", link)
                + EmailLayout.note("This link can be used once and expires in " + lifetime(purpose) + ".")
                + EmailLayout.note(reset
                        ? "Didn't ask for this? You can ignore this email — your password stays unchanged."
                        : "If you weren't expecting this, you can safely ignore this email.");

        return EmailLayout.shell(headline, content);
    }

    /** Plain-text fallback for clients that won't render HTML. */
    public String text(String firstName, InvitationPurpose purpose, String link) {
        boolean reset = purpose == InvitationPurpose.RESET;
        String lead = reset
                ? "We received a request to reset your ClaimLens password."
                : "An account has been created for you on ClaimLens.";
        return "Hi " + safeName(firstName) + ",\n\n" + lead
                + "\n\nSet your password here:\n" + link
                + "\n\nThis link can be used once and expires in " + lifetime(purpose) + "."
                + "\nIf you weren't expecting it, you can ignore this email.\n\n— ClaimLens";
    }

    private String lifetime(InvitationPurpose purpose) {
        return purpose == InvitationPurpose.RESET ? "1 hour" : "7 days";
    }

    private String safeName(String firstName) {
        return (firstName == null || firstName.isBlank()) ? "there" : firstName;
    }
}
