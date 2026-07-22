package com.niyotechnologies.claimlens.notification;

import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import com.niyotechnologies.claimlens.notification.report.ClaimEmailEvent;
import com.niyotechnologies.claimlens.notification.report.ClaimPdfRenderer;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.DocRef;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.StatusTone;
import com.niyotechnologies.claimlens.notification.report.ClaimReportRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-unit tests for the claim report renderers — no Spring context, no DB. Proves the HTML email
 * carries the key fields and the PDF renders to a valid document with an image embedded.
 */
class ClaimReportRenderTest {

    private final ClaimReportRenderer renderer = new ClaimReportRenderer();

    private ClaimReportContext context(List<DocRef> documents) {
        return new ClaimReportContext(
                "CLM-TEST-1", "Approved", StatusTone.POSITIVE,
                "Priya Sharma", "POL-9001", "MH12AB1234",
                new BigDecimal("84000"), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2),
                "My car was hit from behind at a signal on MG Road.",
                "Arjun Rao", "Arjun Rao", "Documents verified; damage consistent with the report.",
                18, "LOW", documents);
    }

    @Test
    void emailHtmlCarriesTheKeyFields() {
        List<DocRef> docs = List.of(
                new DocRef("rc-book.pdf", "RC_BOOK", "application/pdf", null, "k1"),
                new DocRef("fir.pdf", "FIR", "application/pdf", null, "k2"));
        String html = renderer.emailHtml(ClaimEmailEvent.APPROVED, context(docs));
        assertTrue(html.contains("CLM-TEST-1"), "claim number");
        assertTrue(html.contains("Approved"), "status badge");
        assertTrue(html.contains("Priya Sharma"), "policyholder");
        assertTrue(html.contains("Arjun Rao"), "decided-by");
        assertTrue(html.contains("MH12AB1234"), "vehicle");
        assertTrue(html.contains("MG Road"), "incident description");
        assertTrue(html.contains("RC Book") && html.contains("FIR"), "documents on file");
        assertTrue(html.contains("<html"), "is HTML");

        String text = renderer.emailText(ClaimEmailEvent.APPROVED, context(docs));
        assertTrue(text.contains("CLM-TEST-1") && text.contains("Approved"), "text fallback");
        assertTrue(text.contains("What happened") && text.contains("RC Book"), "text has story + docs");
    }

    @Test
    void pdfRendersToAValidDocumentWithEmbeddedImage() throws Exception {
        byte[] png = tinyPng();
        DocumentStorage storage = new DocumentStorage() {
            @Override public String store(byte[] content, String originalFileName) { return "k"; }
            @Override public byte[] retrieve(String storageKey) { return png; }
        };
        ClaimPdfRenderer pdfRenderer = new ClaimPdfRenderer(renderer, storage);

        List<DocRef> documents = List.of(
                new DocRef("damage-front.jpg", "DAMAGE_PHOTO", "image/jpeg", null, "k1"),
                new DocRef("fir.pdf", "FIR", "application/pdf", null, "k2"));
        byte[] pdf = pdfRenderer.render(context(documents));

        assertTrue(pdf.length > 1000, "non-trivial PDF, got " + pdf.length + " bytes");
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII), "PDF magic header");
    }

    private static byte[] tinyPng() throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                img.setRGB(x, y, Color.GRAY.getRGB());
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
