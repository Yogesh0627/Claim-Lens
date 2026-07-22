package com.niyotechnologies.claimlens.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default sender: logs the email instead of transmitting it. Active whenever
 * {@code claimlens.email.provider} is unset or anything other than {@code smtp}, so local dev, CI and
 * the demo run without a mail server.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "claimlens.email.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("[email:log] to={} subject=\"{}\" body=\"{}\"", toEmail, subject, body);
    }

    @Override
    public void send(EmailMessage message) {
        log.info("[email:log] to={} subject=\"{}\" html={}B attachments={}",
                message.to(), message.subject(),
                message.htmlBody() == null ? 0 : message.htmlBody().length(),
                message.attachments().size());
    }
}
