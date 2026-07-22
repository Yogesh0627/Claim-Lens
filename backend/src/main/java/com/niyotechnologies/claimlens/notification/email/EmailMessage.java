package com.niyotechnologies.claimlens.notification.email;

import java.util.List;

/**
 * A rich email: an HTML body (with a plain-text fallback for clients that won't render HTML) and
 * optional attachments. Built by the report layer and handed to an {@link EmailSender}.
 *
 * <p>Every sender degrades gracefully: one that can't do HTML falls back to {@link #textBody()},
 * and one that can't do attachments simply omits them — the message still goes out.
 */
public record EmailMessage(
        String to,
        String subject,
        String textBody,
        String htmlBody,
        List<EmailAttachment> attachments) {

    public EmailMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public static EmailMessage of(String to, String subject, String textBody, String htmlBody) {
        return new EmailMessage(to, subject, textBody, htmlBody, List.of());
    }

    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }
}
