package com.niyotechnologies.claimlens.notification.report;

import com.niyotechnologies.claimlens.notification.email.EmailAttachment;
import com.niyotechnologies.claimlens.notification.email.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the rich claim email: branded HTML body, plain-text fallback, and the "Claim Report" PDF
 * attached. PDF generation is best-effort — if it fails, the email still goes out with the HTML body,
 * so a rendering hiccup never costs the customer their notification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimReportService {

    @Autowired
    private final ClaimReportRenderer renderer;
    @Autowired
    private final ClaimPdfRenderer pdfRenderer;

    public EmailMessage buildEmail(String toEmail, ClaimEmailEvent event, ClaimReportContext ctx) {
        String subject = subjectFor(event, ctx);
        String html = renderer.emailHtml(event, ctx);
        String text = renderer.emailText(event, ctx);

        List<EmailAttachment> attachments = new ArrayList<>();
        try {
            byte[] pdf = pdfRenderer.render(ctx);
            if (pdf != null && pdf.length > 0) {
                String name = "claim-" + safe(ctx.claimNumber()) + ".pdf";
                attachments.add(new EmailAttachment(name, "application/pdf", pdf));
            }
        } catch (Exception e) {
            log.warn("Claim report PDF failed for {} ({}) — sending HTML only",
                    ctx.claimNumber(), e.getMessage());
        }
        return new EmailMessage(toEmail, subject, text, html, attachments);
    }

    private String subjectFor(ClaimEmailEvent event, ClaimReportContext ctx) {
        String c = ctx.claimNumber();
        return switch (event) {
            case SUBMITTED -> "Claim " + c + " received";
            case ASSIGNED -> "New claim assigned — " + c;
            case INFO_REQUESTED -> "More information needed for claim " + c;
            case APPROVED -> "Claim " + c + " approved ✅";
            case REJECTED -> "An update on claim " + c;
        };
    }

    private String safe(String s) {
        return s == null ? "report" : s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
