package com.niyotechnologies.claimlens.notification.email;

/**
 * A binary attachment for a rich email (e.g. the generated claim-report PDF).
 * {@code content} is the raw bytes; each sender encodes it as its transport needs
 * (Resend wants base64, JavaMail wants a DataSource).
 */
public record EmailAttachment(String fileName, String contentType, byte[] content) {
}
