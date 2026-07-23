package com.niyotechnologies.claimlens.notification;

import com.niyotechnologies.claimlens.auth.enums.InvitationPurpose;
import com.niyotechnologies.claimlens.notification.email.AccountEmailRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-unit tests for the invitation / reset emails — no Spring context, no mail server. */
class AccountEmailRenderTest {

    private static final String LINK = "https://claimlens.app/set-password?token=abc123";
    private final AccountEmailRenderer renderer = new AccountEmailRenderer();

    @Test
    void invitationCarriesTheLinkAsBothButtonAndCopyablePlainUrl() {
        String html = renderer.html("Riya", InvitationPurpose.INVITE, LINK);

        assertTrue(html.contains("Welcome to ClaimLens"), "headline");
        assertTrue(html.contains("Riya"), "greeting");
        assertTrue(html.contains("Set your password"), "call to action");
        assertTrue(html.contains("7 days"), "invitation lifetime");
        assertTrue(html.contains("<html"), "is HTML");
        // Buttons get mangled by some gateways, so the raw URL must be present too — twice overall
        // (href + copyable text).
        assertTrue(html.split("set-password\\?token=abc123", -1).length - 1 >= 2,
                "link must appear as both a button and a pasteable URL");
    }

    @Test
    void resetDiffersFromInvitationInWordingAndLifetime() {
        String html = renderer.html("Riya", InvitationPurpose.RESET, LINK);

        assertTrue(html.contains("Reset your password"), "reset headline");
        assertTrue(html.contains("1 hour"), "reset lifetime is short");
        assertFalse(html.contains("7 days"), "must not claim the invitation lifetime");
        assertTrue(renderer.subject(InvitationPurpose.RESET).toLowerCase().contains("reset"));
    }

    @Test
    void plainTextFallbackStillContainsTheLink() {
        String text = renderer.text("Riya", InvitationPurpose.INVITE, LINK);
        assertTrue(text.contains(LINK), "a text-only client must still be able to use the link");
        assertFalse(text.contains("<"), "plain text must not carry markup");
    }

    @Test
    void aMissingFirstNameDegradesToAFriendlyGreeting() {
        assertTrue(renderer.html(null, InvitationPurpose.INVITE, LINK).contains("Hi there"));
        assertTrue(renderer.text("  ", InvitationPurpose.INVITE, LINK).contains("Hi there"));
    }
}
