package com.niyotechnologies.claimlens.notification;

import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import com.niyotechnologies.claimlens.notification.email.EmailMessage;
import com.niyotechnologies.claimlens.notification.email.ResendEmailSender;
import com.niyotechnologies.claimlens.notification.report.ClaimEmailEvent;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.DocRef;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.StatusTone;
import com.niyotechnologies.claimlens.notification.report.ClaimPdfRenderer;
import com.niyotechnologies.claimlens.notification.report.ClaimReportRenderer;
import com.niyotechnologies.claimlens.notification.report.ClaimReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MANUAL: renders every claim email (branded HTML + attached PDF, with sample photos) and sends them
 * to a real inbox so the rendering / responsiveness can be eyeballed. Never runs in the normal suite —
 * only when {@code SEND_SAMPLE_EMAILS=true} and {@code RESEND_API_KEY} are set. Recipient comes from
 * {@code SAMPLE_EMAIL_TO}.
 */
@EnabledIfEnvironmentVariable(named = "SEND_SAMPLE_EMAILS", matches = "true")
class SendSampleClaimEmailsManualTest {

    // Real, hosted sample photos: shown inline in the email (publicUrl) and embedded in the PDF (bytes).
    private static final Map<String, String> PHOTOS = Map.of(
            "photo-front", "https://picsum.photos/seed/claimfront/640/420",
            "photo-side", "https://picsum.photos/seed/claimside/640/420",
            "photo-rc", "https://picsum.photos/seed/claimrc/640/420");

    @Test
    void sendAllClaimEmailsWithPdf() throws Exception {
        String apiKey = env("RESEND_API_KEY");
        String from = envOr("EMAIL_FROM", "Claimlens <noreply@mail.yogeshchauhan.dev>");
        String to = envOr("SAMPLE_EMAIL_TO", "chauhanyogesh950@gmail.com");
        String baseUrl = envOr("RESEND_BASE_URL", "https://api.resend.com");

        ClaimReportRenderer renderer = new ClaimReportRenderer();
        ClaimPdfRenderer pdfRenderer = new ClaimPdfRenderer(renderer, new DownloadingStorage());
        ClaimReportService reportService = new ClaimReportService(renderer, pdfRenderer);
        ResendEmailSender sender = new ResendEmailSender(baseUrl, apiKey, from);

        for (ClaimEmailEvent event : ClaimEmailEvent.values()) {
            EmailMessage msg = reportService.buildEmail(to, event, contextFor(event));
            sender.send(msg);
            System.out.printf("Sent %-14s subject=\"%s\" attachments=%d%n",
                    event, msg.subject(), msg.attachments().size());
            Thread.sleep(600);   // stay under Resend's rate limit
        }
        System.out.println("All 5 sample claim emails sent to " + to);
    }

    private ClaimReportContext contextFor(ClaimEmailEvent event) {
        List<DocRef> documents = List.of(
                new DocRef("damage-front.jpg", "DAMAGE_PHOTO", "image/jpeg", PHOTOS.get("photo-front"), "photo-front"),
                new DocRef("damage-side.jpg", "DAMAGE_PHOTO", "image/jpeg", PHOTOS.get("photo-side"), "photo-side"),
                new DocRef("driving-license.jpg", "DRIVING_LICENSE", "image/jpeg", PHOTOS.get("photo-rc"), "photo-rc"),
                new DocRef("rc-book.pdf", "RC_BOOK", "application/pdf", null, "doc-rc"),
                new DocRef("fir.pdf", "FIR", "application/pdf", null, "doc-fir"));

        String incident = "While stopped at the MG Road signal, another vehicle rear-ended my car. "
                + "The rear bumper and boot are dented. I've filed an FIR and attached the repair estimate.";

        String statusLabel;
        StatusTone tone;
        String investigator = null, decidedBy = null, reason = null;
        Integer score = 18;
        String risk = "LOW";
        switch (event) {
            case SUBMITTED -> { statusLabel = "Submitted"; tone = StatusTone.INFO; score = null; risk = null; }
            case ASSIGNED -> { statusLabel = "Under investigation"; tone = StatusTone.INFO; investigator = "Arjun Rao"; score = 42; risk = "MEDIUM"; }
            case INFO_REQUESTED -> { statusLabel = "Waiting for customer"; tone = StatusTone.INFO; investigator = "Arjun Rao"; reason = "Please re-upload a clearer photo of the RC book — the current one is blurred."; }
            case APPROVED -> { statusLabel = "Approved"; tone = StatusTone.POSITIVE; investigator = "Arjun Rao"; decidedBy = "Arjun Rao"; reason = "Documents verified; damage consistent with the FIR and the repair estimate."; }
            case REJECTED -> { statusLabel = "Rejected"; tone = StatusTone.NEGATIVE; investigator = "Arjun Rao"; decidedBy = "Arjun Rao"; reason = "The reported incident date falls outside the policy's active period."; score = 81; risk = "HIGH"; }
            default -> { statusLabel = "Submitted"; tone = StatusTone.NEUTRAL; }
        }

        return new ClaimReportContext(
                "CLM-2026-0042", statusLabel, tone,
                "Priya Sharma", "POL-9001", "MH 12 AB 1234",
                new BigDecimal("84000"), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2),
                incident, investigator, decidedBy, reason, score, risk, documents);
    }

    /** DocumentStorage whose retrieve() downloads the sample photo bytes for PDF embedding. */
    private static final class DownloadingStorage implements DocumentStorage {
        private final HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        private final Map<String, byte[]> cache = new HashMap<>();

        @Override
        public String store(byte[] content, String originalFileName) {
            return "unused";
        }

        @Override
        public byte[] retrieve(String storageKey) {
            return cache.computeIfAbsent(storageKey, key -> {
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create(PHOTOS.get(key))).GET().build();
                    return http.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
                } catch (Exception e) {
                    throw new RuntimeException("Could not fetch sample photo " + key, e);
                }
            });
        }

        @Override
        public Optional<String> publicUrl(String storageKey) {
            return Optional.ofNullable(PHOTOS.get(storageKey));
        }
    }

    private static String env(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException(name + " must be set to run this manual send");
        }
        return v;
    }

    private static String envOr(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
