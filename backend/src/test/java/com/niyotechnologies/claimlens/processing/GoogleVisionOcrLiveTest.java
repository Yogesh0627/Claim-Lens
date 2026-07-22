package com.niyotechnologies.claimlens.processing;

import com.niyotechnologies.claimlens.processing.ocr.GoogleVisionOcrClient;
import com.niyotechnologies.claimlens.processing.ocr.OcrExtraction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real end-to-end check against Google Cloud Vision. Skipped unless GOOGLE_VISION_API_KEY is set, so
 * CI/offline stays green. Draws a text image and asserts Vision reads back the registration + policy
 * numbers — proving the in-JVM client (not just the Python service) works against the live API.
 */
@EnabledIfEnvironmentVariable(named = "GOOGLE_VISION_API_KEY", matches = ".+")
class GoogleVisionOcrLiveTest {

    @Test
    void readsRegistrationAndPolicyFromAnImage() throws Exception {
        String key = System.getenv("GOOGLE_VISION_API_KEY");
        GoogleVisionOcrClient client =
                new GoogleVisionOcrClient("https://vision.googleapis.com/v1", key);

        byte[] png = textImage("CLAIMLENS OCR TEST", "Policy No: DEMO-POL-1", "MH 12 AB 1234");
        OcrExtraction result = client.extract(png, "rc.png", "image/png", "RC_BOOK");

        assertThat(result.engine()).isEqualTo("vision");
        assertThat(result.text()).contains("MH 12 AB 1234").contains("DEMO-POL-1");
        assertThat(result.registrationNumbers()).contains("MH12AB1234");
        assertThat(result.policyNumbers()).contains("DEMO-POL-1");
    }

    private static byte[] textImage(String... lines) throws Exception {
        BufferedImage img = new BufferedImage(760, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 760, 300);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        int y = 70;
        for (String line : lines) {
            g.drawString(line, 30, y);
            y += 70;
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
