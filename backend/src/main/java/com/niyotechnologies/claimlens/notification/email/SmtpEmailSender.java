package com.niyotechnologies.claimlens.notification.email;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Real SMTP delivery via Spring's {@link JavaMailSender} (configure {@code spring.mail.*}). Active
 * only when {@code claimlens.email.provider=smtp}. Best-effort: a send failure is logged and
 * swallowed so it never rolls back the notification that triggered it.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "claimlens.email.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    @Autowired
    private final JavaMailSender mailSender;

    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${claimlens.email.from:no-reply@claimlens.app}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("[email:smtp] failed to send to={} subject=\"{}\": {}", toEmail, subject, e.getMessage());
        }
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            // multipart=true so we can carry both an HTML body and file attachments.
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            String text = message.textBody() == null ? "" : message.textBody();
            if (message.htmlBody() != null && !message.htmlBody().isBlank()) {
                helper.setText(text, message.htmlBody());   // (plain, html) — clients pick the richest they support
            } else {
                helper.setText(text, false);
            }
            for (EmailAttachment a : message.attachments()) {
                helper.addAttachment(a.fileName(), new ByteArrayResource(a.content()), a.contentType());
            }
            mailSender.send(mime);
        } catch (Exception e) {
            log.warn("[email:smtp] failed to send rich email to={} subject=\"{}\": {}",
                    message.to(), message.subject(), e.getMessage());
        }
    }
}
