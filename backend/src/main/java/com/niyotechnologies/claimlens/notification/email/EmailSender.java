package com.niyotechnologies.claimlens.notification.email;

/**
 * Second delivery channel behind {@code NotificationService}. Swappable per the project's standard
 * conditional-client pattern: a no-op {@link LogEmailSender} is the default so tests and offline runs
 * stay green, and {@link SmtpEmailSender} activates only when {@code claimlens.email.provider=smtp}.
 * Implementations must be best-effort — an email failure must never break the caller's transaction.
 */
public interface EmailSender {

    /** Plain-text send — the low-level primitive every sender must implement. */
    void send(String toEmail, String subject, String body);

    /**
     * Rich send (HTML body + optional attachments). The default degrades to the plain-text primitive,
     * so a sender only overrides this if it can actually do HTML/attachments (Resend, SMTP). Callers
     * always get a delivery attempt regardless of the active provider.
     */
    default void send(EmailMessage message) {
        send(message.to(), message.subject(),
                message.textBody() != null ? message.textBody() : "");
    }
}
