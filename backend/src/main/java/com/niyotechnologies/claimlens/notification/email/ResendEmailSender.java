package com.niyotechnologies.claimlens.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real email via the Resend HTTP API (https://resend.com). Active only when
 * {@code claimlens.email.provider=resend}. The API key is a secret — supplied via env
 * ({@code RESEND_API_KEY}), never committed. Best-effort like the SMTP sender: a send failure is
 * logged and swallowed so it can never roll back the notification that triggered it.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "claimlens.email.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String from;

    public ResendEmailSender(
            @Value("${claimlens.email.resend.base-url:https://api.resend.com}") String baseUrl,
            @Value("${claimlens.email.resend.api-key:}") String apiKey,
            @Value("${claimlens.email.from:Claimlens <noreply@mail.yogeshchauhan.dev>}") String from) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.from = from;
    }

    @Override
    public void send(String toEmail, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(toEmail),
                "subject", subject,
                "text", body);
        post(payload, toEmail, subject);
    }

    @Override
    public void send(EmailMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", from);
        payload.put("to", List.of(message.to()));
        payload.put("subject", message.subject());
        if (message.htmlBody() != null && !message.htmlBody().isBlank()) {
            payload.put("html", message.htmlBody());
        }
        if (message.textBody() != null && !message.textBody().isBlank()) {
            payload.put("text", message.textBody());
        }
        if (message.hasAttachments()) {
            // Resend takes attachments as { filename, content(base64) }.
            List<Map<String, Object>> attachments = new ArrayList<>();
            for (EmailAttachment a : message.attachments()) {
                attachments.add(Map.of(
                        "filename", a.fileName(),
                        "content", Base64.getEncoder().encodeToString(a.content())));
            }
            payload.put("attachments", attachments);
        }
        post(payload, message.to(), message.subject());
    }

    private void post(Map<String, Object> payload, String toEmail, String subject) {
        try {
            restClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("[email:resend] failed to send to={} subject=\"{}\": {}", toEmail, subject, e.getMessage());
        }
    }
}
