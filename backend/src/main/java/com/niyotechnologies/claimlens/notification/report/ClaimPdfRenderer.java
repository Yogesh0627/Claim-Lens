package com.niyotechnologies.claimlens.notification.report;

import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext.DocRef;
import com.niyotechnologies.claimlens.notification.report.ClaimReportRenderer.EmbeddedImage;
import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the "Claim Report" PDF via openhtmltopdf. Image bytes are pulled from {@link DocumentStorage}
 * and served to the renderer under a private {@code claimimg://} scheme (a custom
 * {@link FSStreamFactory}) — deterministic in-memory embedding that works for any storage backend,
 * with no data-URI parsing or temp files. At most {@value #MAX_IMAGES} images are embedded to keep the
 * attachment small.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimPdfRenderer {

    private static final int MAX_IMAGES = 6;
    private static final String SCHEME = "claimimg";

    @Autowired
    private final ClaimReportRenderer renderer;
    @Autowired
    private final DocumentStorage documentStorage;

    /** Build the report PDF. Returns the bytes; callers treat a failure as best-effort (no PDF). */
    public byte[] render(ClaimReportContext ctx) throws Exception {
        Map<String, byte[]> assets = new HashMap<>();
        List<EmbeddedImage> embedded = new ArrayList<>();
        int i = 0;
        for (DocRef img : ctx.imageDocuments()) {
            if (i >= MAX_IMAGES) {
                break;
            }
            try {
                byte[] bytes = documentStorage.retrieve(img.storageKey());
                if (bytes != null && bytes.length > 0) {
                    String uri = SCHEME + "://" + i;
                    assets.put(uri, bytes);
                    embedded.add(new EmbeddedImage(img.fileName(), uri));
                    i++;
                }
            } catch (Exception e) {
                log.debug("Skipping report image {}: {}", img.fileName(), e.getMessage());
            }
        }

        String xhtml = renderer.pdfXhtml(ctx, embedded);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            if (!assets.isEmpty()) {
                builder.useProtocolsStreamImplementation(new InMemoryStreamFactory(assets), SCHEME);
            }
            builder.withHtmlContent(xhtml, "");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    /** Serves the pre-fetched image bytes for the {@code claimimg://} scheme. */
    private record InMemoryStreamFactory(Map<String, byte[]> assets) implements FSStreamFactory {
        @Override
        public FSStream getUrl(String uri) {
            byte[] bytes = assets.getOrDefault(uri, new byte[0]);
            return new FSStream() {
                @Override
                public InputStream getStream() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public Reader getReader() {
                    return new InputStreamReader(getStream(), StandardCharsets.UTF_8);
                }
            };
        }
    }
}
