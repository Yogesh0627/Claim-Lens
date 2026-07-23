package com.niyotechnologies.claimlens.notification;

import com.niyotechnologies.claimlens.auth.enums.InvitationPurpose;
import com.niyotechnologies.claimlens.notification.email.AccountEmailRenderer;
import com.niyotechnologies.claimlens.notification.email.EmailMessage;
import com.niyotechnologies.claimlens.notification.email.ResendEmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * MANUAL: sends the real invitation and password-reset emails to a live inbox so their rendering can
 * be eyeballed. Never runs in the normal suite — only with {@code SEND_SAMPLE_EMAILS=true} and a
 * {@code RESEND_API_KEY}.
 */
@EnabledIfEnvironmentVariable(named = "SEND_SAMPLE_EMAILS", matches = "true")
class SendSampleAccountEmailsManualTest {

    @Test
    void sendInvitationAndResetEmails() throws Exception {
        String apiKey = System.getenv("RESEND_API_KEY");
        String from = envOr("EMAIL_FROM", "Claimlens <noreply@mail.yogeshchauhan.dev>");
        String to = envOr("SAMPLE_EMAIL_TO", "chauhanyogesh950@gmail.com");
        String appUrl = envOr("APP_BASE_URL", "http://localhost:3000");

        AccountEmailRenderer renderer = new AccountEmailRenderer();
        ResendEmailSender sender =
                new ResendEmailSender(envOr("RESEND_BASE_URL", "https://api.resend.com"), apiKey, from);

        for (InvitationPurpose purpose : InvitationPurpose.values()) {
            String link = appUrl + "/set-password?token=sample-token-" + purpose.name().toLowerCase();
            sender.send(EmailMessage.of(
                    to,
                    renderer.subject(purpose),
                    renderer.text("Yogesh", purpose, link),
                    renderer.html("Yogesh", purpose, link)));
            System.out.printf("Sent %-6s subject=\"%s\"%n", purpose, renderer.subject(purpose));
            Thread.sleep(600);
        }
        System.out.println("Both account emails sent to " + to);
    }

    private static String envOr(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
