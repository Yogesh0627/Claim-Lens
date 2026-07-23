package com.niyotechnologies.claimlens.notification.report;

import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.DocRef;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.StatusTone;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Turns a {@link ClaimReportContext} into presentation output — the branded HTML email body, its
 * plain-text fallback, and the XHTML the PDF is rendered from. Pure functions with no I/O or Spring
 * dependencies, so they're trivially unit-testable; the PDF's images are passed in already-encoded
 * (see {@link ClaimPdfRenderer}), keeping storage access out of here.
 */
@Component
public class ClaimReportRenderer {

    // Matches the app's theme, which is fully neutral (every CSS token is chroma 0 — there is no
    // accent hue). BRAND is --primary oklch(0.205 0 0) = #171717, resolved to hex because email
    // clients can't read CSS variables. Status colours below stay green/amber/red: those carry
    // meaning rather than branding.
    private static final String BRAND = "#171717";
    private static final String BRAND_DARK = "#404040";
    private static final String INK = "#0a0a0a";
    private static final String MUTED = "#737373";
    private static final String LINE = "#e5e5e5";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    // ---------------------------------------------------------------- HTML email

    public String emailHtml(ClaimEmailEvent event, ClaimReportContext ctx) {
        String badge = statusBadgeHtml(ctx.statusLabel(), ctx.statusTone());

        StringBuilder rows = new StringBuilder();
        detailRow(rows, "Claim number", esc(ctx.claimNumber()));
        detailRow(rows, "Policyholder", esc(ctx.customerName()));
        detailRow(rows, "Policy number", esc(ctx.policyNumber()));
        detailRow(rows, "Vehicle", esc(ctx.vehicleRegistration()));
        detailRow(rows, "Claim amount", esc(money(ctx.claimAmount())));
        detailRow(rows, "Incident date", esc(date(ctx.incidentDate())));
        detailRow(rows, "Reported date", esc(date(ctx.reportedDate())));
        detailRow(rows, "Investigator", esc(ctx.investigatorName()));
        detailRow(rows, "Decided by", esc(ctx.decidedByName()));
        if (ctx.fraudRiskLevel() != null) {
            detailRow(rows, "Fraud risk", riskChipHtml(ctx.fraudScore(), ctx.fraudRiskLevel()));
        }

        String incident = calloutHtml("What happened", ctx.incidentDescription(), BRAND);
        String reason = calloutHtml("Note from the investigator", ctx.decisionReason(), "#f59e0b");
        String documents = emailDocumentsHtml(ctx.documents());
        String images = emailImagesHtml(ctx.imageDocuments());

        return """
                <!doctype html>
                <html><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/></head>
                <body style="margin:0;padding:0;background:#f4f5f7;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f5f7;padding:24px 12px;">
                  <tr><td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,.08);font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                      <tr><td style="background:linear-gradient(135deg,%s,%s);padding:22px 32px;">
                        <div style="font-size:18px;font-weight:800;color:#ffffff;letter-spacing:-.01em;">🛡️ ClaimLens</div>
                        <div style="font-size:12px;color:#a3a3a3;margin-top:2px;">Motor claims, automated &amp; accountable</div>
                      </td></tr>
                      <tr><td style="padding:28px 32px 8px;">
                        <div style="font-size:22px;font-weight:800;color:%s;line-height:1.25;">%s</div>
                        <div style="margin-top:8px;">%s</div>
                        <p style="font-size:15px;color:%s;line-height:1.6;margin:14px 0 0;">%s</p>
                      </td></tr>
                      %s
                      <tr><td style="padding:20px 32px 4px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid %s;border-radius:12px;">
                          %s
                        </table>
                      </td></tr>
                      %s
                      %s
                      %s
                      <tr><td style="padding:24px 32px 30px;">
                        <div style="border-top:1px solid %s;padding-top:16px;font-size:12px;color:%s;line-height:1.6;">
                          A detailed PDF report is attached to this email. This is an automated message from a
                          <strong>demo</strong> deployment of ClaimLens — please don't reply.
                        </div>
                      </td></tr>
                    </table>
                    <div style="font-size:11px;color:#9ca3af;margin-top:14px;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;">© ClaimLens · a portfolio project</div>
                  </td></tr>
                </table>
                </body></html>
                """.formatted(
                BRAND, BRAND_DARK, INK, esc(event.headline()), badge, INK, esc(event.intro()),
                incident, LINE, rows, reason, documents, images, LINE, MUTED);
    }

    private String calloutHtml(String label, String body, String accent) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return """
                <tr><td style="padding:16px 32px 0;">
                  <div style="background:#f8fafc;border-left:3px solid %s;border-radius:8px;padding:14px 16px;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:%s;">%s</div>
                    <div style="font-size:14px;color:%s;margin-top:6px;line-height:1.6;">%s</div>
                  </div></td></tr>
                """.formatted(accent, MUTED, esc(label), INK, esc(body));
    }

    private String emailDocumentsHtml(List<DocRef> docs) {
        if (docs.isEmpty()) {
            return "";
        }
        StringBuilder rows = new StringBuilder();
        for (DocRef d : docs) {
            String icon = d.isImage() ? "🖼️" : "📄";
            rows.append("""
                    <tr><td style="padding:9px 14px;border-bottom:1px solid %s;font-size:13px;">
                      <span style="margin-right:8px;">%s</span><strong style="color:%s;">%s</strong>
                      <span style="color:%s;"> · %s</span>
                    </td></tr>
                    """.formatted(LINE, icon, INK, esc(humanizeDocType(d.documentType())), MUTED, esc(d.fileName())));
        }
        return """
                <tr><td style="padding:18px 32px 0;">
                  <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:%s;padding-left:2px;">Documents on file</div>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid %s;border-radius:12px;margin-top:8px;">%s</table>
                </td></tr>
                """.formatted(MUTED, LINE, rows);
    }

    private String emailImagesHtml(List<DocRef> images) {
        List<DocRef> shown = images.stream()
                .filter(i -> i.publicUrl() != null && !i.publicUrl().isBlank())
                .limit(6).toList();
        if (shown.isEmpty()) {
            return "";
        }
        StringBuilder cells = new StringBuilder();
        for (DocRef img : shown) {
            cells.append("""
                    <td style="padding:4px;" valign="top">
                      <img src="%s" alt="%s" width="168" style="width:168px;height:112px;object-fit:cover;border-radius:8px;border:1px solid %s;display:block;"/>
                    </td>
                    """.formatted(esc(img.publicUrl()), esc(img.fileName()), LINE));
        }
        return """
                <tr><td style="padding:18px 28px 0;">
                  <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:%s;padding-left:6px;">Photos</div>
                  <table role="presentation" cellpadding="0" cellspacing="0" style="margin-top:8px;"><tr>%s</tr></table>
                </td></tr>
                """.formatted(MUTED, cells);
    }

    private void detailRow(StringBuilder sb, String label, String valueHtml) {
        if (valueHtml == null || valueHtml.isBlank()) {
            return;
        }
        sb.append("""
                <tr>
                  <td style="padding:11px 16px;font-size:13px;color:%s;border-bottom:1px solid %s;width:42%%;">%s</td>
                  <td style="padding:11px 16px;font-size:14px;color:%s;font-weight:600;border-bottom:1px solid %s;">%s</td>
                </tr>
                """.formatted(MUTED, LINE, esc(label), INK, LINE, valueHtml));
    }

    private String statusBadgeHtml(String label, StatusTone tone) {
        String[] c = toneColors(tone);
        return """
                <span style="display:inline-block;background:%s;color:%s;border:1px solid %s;border-radius:999px;padding:5px 12px;font-size:12px;font-weight:700;letter-spacing:.02em;">%s</span>
                """.formatted(c[0], c[1], c[2], esc(label));
    }

    private String riskChipHtml(Integer score, String risk) {
        StatusTone tone = switch (risk == null ? "" : risk.toUpperCase(Locale.ENGLISH)) {
            case "HIGH" -> StatusTone.NEGATIVE;
            case "MEDIUM" -> StatusTone.INFO;
            case "LOW" -> StatusTone.POSITIVE;
            default -> StatusTone.NEUTRAL;
        };
        String[] c = toneColors(tone);
        String text = risk + (score == null ? "" : " · " + score + "/100");
        return """
                <span style="display:inline-block;background:%s;color:%s;border:1px solid %s;border-radius:6px;padding:2px 8px;font-size:12px;font-weight:700;">%s</span>
                """.formatted(c[0], c[1], c[2], esc(text));
    }

    /** {bg, text, border} per tone. */
    private String[] toneColors(StatusTone tone) {
        return switch (tone) {
            case POSITIVE -> new String[]{"#ecfdf5", "#047857", "#a7f3d0"};
            case NEGATIVE -> new String[]{"#fef2f2", "#b91c1c", "#fecaca"};
            case INFO -> new String[]{"#eff6ff", "#1d4ed8", "#bfdbfe"};
            case NEUTRAL -> new String[]{"#f3f4f6", "#374151", "#e5e7eb"};
        };
    }

    // ---------------------------------------------------------------- plain-text fallback

    public String emailText(ClaimEmailEvent event, ClaimReportContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(event.headline()).append("\n\n").append(event.intro()).append("\n\n");
        line(sb, "Claim number", ctx.claimNumber());
        line(sb, "Status", ctx.statusLabel());
        line(sb, "Policyholder", ctx.customerName());
        line(sb, "Policy number", ctx.policyNumber());
        line(sb, "Vehicle", ctx.vehicleRegistration());
        line(sb, "Claim amount", money(ctx.claimAmount()));
        line(sb, "Incident date", date(ctx.incidentDate()));
        line(sb, "Investigator", ctx.investigatorName());
        line(sb, "Decided by", ctx.decidedByName());
        if (ctx.fraudRiskLevel() != null) {
            line(sb, "Fraud risk", ctx.fraudRiskLevel() + (ctx.fraudScore() == null ? "" : " (" + ctx.fraudScore() + "/100)"));
        }
        if (ctx.incidentDescription() != null && !ctx.incidentDescription().isBlank()) {
            sb.append("\nWhat happened:\n").append(ctx.incidentDescription()).append("\n");
        }
        if (ctx.decisionReason() != null && !ctx.decisionReason().isBlank()) {
            sb.append("\nInvestigator note: ").append(ctx.decisionReason()).append("\n");
        }
        if (!ctx.documents().isEmpty()) {
            sb.append("\nDocuments on file:\n");
            for (DocRef d : ctx.documents()) {
                sb.append("  - ").append(humanizeDocType(d.documentType()))
                        .append(" (").append(d.fileName()).append(")\n");
            }
        }
        sb.append("\nA detailed PDF report is attached. — ClaimLens (demo, automated message)\n");
        return sb.toString();
    }

    private void line(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }

    // ---------------------------------------------------------------- PDF (XHTML, strict)

    /** An image already encoded as a {@code data:} URI (or private scheme), ready to embed in the PDF. */
    public record EmbeddedImage(String fileName, String dataUri) {
    }

    public String pdfXhtml(ClaimReportContext ctx, List<EmbeddedImage> embedded) {
        StringBuilder rows = new StringBuilder();
        pdfRow(rows, "Policyholder", ctx.customerName());
        pdfRow(rows, "Policy number", ctx.policyNumber());
        pdfRow(rows, "Vehicle", ctx.vehicleRegistration());
        pdfRow(rows, "Claim amount", moneyAscii(ctx.claimAmount()));
        pdfRow(rows, "Incident date", date(ctx.incidentDate()));
        pdfRow(rows, "Reported date", date(ctx.reportedDate()));
        pdfRow(rows, "Investigator", ctx.investigatorName());
        pdfRow(rows, "Decided by", ctx.decidedByName());
        if (ctx.fraudRiskLevel() != null) {
            String risk = ctx.fraudRiskLevel() + (ctx.fraudScore() == null ? "" : " (" + ctx.fraudScore() + "/100)");
            pdfRow(rows, "Fraud risk", risk);
        }

        String incident = pdfNote("What happened", ctx.incidentDescription());
        String reason = pdfNote("Investigator note", ctx.decisionReason());

        StringBuilder docRows = new StringBuilder();
        for (DocRef d : ctx.documents()) {
            docRows.append("<tr><td class=\"k\">").append(esc(humanizeDocType(d.documentType())))
                    .append("</td><td class=\"v\">").append(esc(d.fileName())).append("</td></tr>");
        }
        String docsBlock = ctx.documents().isEmpty() ? ""
                : "<div class=\"section\">Documents on file</div><table class=\"kv\">" + docRows + "</table>";

        StringBuilder imgs = new StringBuilder();
        for (EmbeddedImage e : embedded) {
            imgs.append("<div class=\"img\"><img src=\"").append(e.dataUri()).append("\" />")
                    .append("<div class=\"cap\">").append(esc(e.fileName())).append("</div></div>");
        }
        String imagesBlock = embedded.isEmpty() ? ""
                : "<div class=\"section\">Photos</div><div class=\"imgs\">" + imgs + "</div>";

        String[] c = toneColors(ctx.statusTone());

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml"><head><style>
                @page { size: A4; margin: 32pt 36pt; }
                body { font-family: sans-serif; color: %s; font-size: 11pt; }
                .head { border-bottom: 3pt solid %s; padding-bottom: 10pt; margin-bottom: 6pt; }
                .brand { color: %s; font-size: 18pt; font-weight: bold; }
                .sub { color: %s; font-size: 9pt; }
                .title { font-size: 15pt; font-weight: bold; margin: 16pt 0 4pt; }
                .badge { display: inline-block; background: %s; color: %s; border: 1pt solid %s;
                         border-radius: 4pt; padding: 3pt 9pt; font-size: 9pt; font-weight: bold; }
                table.kv { width: 100%%; border-collapse: collapse; margin-top: 12pt; }
                table.kv td { border: 1pt solid %s; padding: 7pt 10pt; font-size: 10.5pt; }
                td.k { color: %s; width: 38%%; }
                td.v { font-weight: bold; }
                .note { margin-top: 14pt; background: #f9fafb; border-left: 3pt solid %s; border-radius: 4pt; padding: 10pt 12pt; }
                .note-h { font-size: 8.5pt; font-weight: bold; color: %s; text-transform: uppercase; margin-bottom: 3pt; }
                .section { margin-top: 18pt; font-size: 9pt; font-weight: bold; color: %s; text-transform: uppercase; }
                .imgs { margin-top: 8pt; }
                .img { display: inline-block; width: 150pt; margin: 0 6pt 6pt 0; vertical-align: top; }
                .img img { width: 150pt; height: 105pt; border: 1pt solid %s; }
                .cap { font-size: 7.5pt; color: %s; margin-top: 2pt; }
                .foot { margin-top: 22pt; border-top: 1pt solid %s; padding-top: 8pt; font-size: 8pt; color: %s; }
                </style></head><body>
                <div class="head"><div class="brand">ClaimLens</div><div class="sub">Motor claims - automated &amp; accountable</div></div>
                <div class="title">Claim Report - %s</div>
                <div><span class="badge">%s</span></div>
                %s
                <table class="kv">%s</table>
                %s
                %s
                %s
                <div class="foot">Generated by ClaimLens (demo deployment). Claim %s. This document reflects the claim state at the time of sending.</div>
                </body></html>
                """.formatted(
                INK, BRAND, BRAND, MUTED,
                c[0], c[1], c[2],
                LINE, MUTED, BRAND, MUTED, MUTED, LINE, MUTED, LINE, MUTED,
                esc(ctx.claimNumber()), esc(ctx.statusLabel()), incident, rows, reason, docsBlock, imagesBlock,
                esc(ctx.claimNumber()));
    }

    private String pdfNote(String label, String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return "<div class=\"note\"><div class=\"note-h\">" + esc(label) + "</div><div>"
                + esc(body) + "</div></div>";
    }

    private void pdfRow(StringBuilder sb, String k, String v) {
        if (v != null && !v.isBlank()) {
            sb.append("<tr><td class=\"k\">").append(esc(k)).append("</td><td class=\"v\">")
                    .append(esc(v)).append("</td></tr>");
        }
    }

    // ---------------------------------------------------------------- helpers

    /** {@code RC_BOOK} -> {@code RC Book}, {@code FIR} -> {@code FIR}, {@code repair-estimate} -> {@code Repair Estimate}. */
    private String humanizeDocType(String type) {
        if (type == null || type.isBlank()) {
            return "Document";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : type.trim().replace('-', '_').split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (part.length() <= 3) {
                sb.append(part.toUpperCase(Locale.ENGLISH));
            } else {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase(Locale.ENGLISH));
            }
        }
        return sb.length() == 0 ? "Document" : sb.toString();
    }

    private String money(BigDecimal amount) {
        return amount == null ? null : "₹ " + amountDigits(amount);
    }

    /** ASCII currency for the PDF — openhtmltopdf's built-in Helvetica has no ₹ glyph. */
    private String moneyAscii(BigDecimal amount) {
        return amount == null ? null : "Rs " + amountDigits(amount);
    }

    private String amountDigits(BigDecimal amount) {
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.of("en", "IN"));
        fmt.setMaximumFractionDigits(2);
        return fmt.format(amount);
    }

    private String date(LocalDate d) {
        return d == null ? null : DATE.format(d);
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
