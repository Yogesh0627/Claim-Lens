package com.niyotechnologies.claimlens.notification.email;

/**
 * The branded shell every transactional email sits in — header, centred card, footer — so account
 * mail (invitations, password resets) looks like the claim mail rather than a different product.
 *
 * <p>Email clients strip {@code <style>} blocks and ignore most modern CSS, so everything here is
 * table-based with inline styles. That is not carelessness; it is what renders in Outlook.
 *
 * <p>{@code ClaimReportRenderer} predates this helper and still builds its own copy of the shell.
 * It could adopt this, but that email is already in production and visually reviewed, so it is left
 * alone deliberately rather than refactored for tidiness.
 */
public final class EmailLayout {

    // Mirrors the app's theme, which is fully neutral — every CSS token is chroma 0, no accent hue.
    // BRAND is --primary oklch(0.205 0 0) = #171717; the rest are the matching neutral steps. Email
    // clients can't read CSS variables, so these are the resolved light-theme values.
    public static final String BRAND = "#171717";
    public static final String BRAND_DARK = "#404040";
    public static final String INK = "#0a0a0a";
    public static final String MUTED = "#737373";

    private EmailLayout() {
    }

    /** Wraps {@code contentHtml} (already-escaped markup) in the branded card. */
    public static String shell(String headline, String contentHtml) {
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
                      </td></tr>
                      %s
                      <tr><td style="padding:24px 32px 30px;">
                        <div style="border-top:1px solid #e5e5e5;padding-top:16px;font-size:12px;color:%s;line-height:1.6;">
                          This is an automated message from a <strong>demo</strong> deployment of
                          ClaimLens — please don't reply.
                        </div>
                      </td></tr>
                    </table>
                    <div style="font-size:11px;color:#9ca3af;margin-top:14px;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;">© ClaimLens · a portfolio project</div>
                  </td></tr>
                </table>
                </body></html>
                """.formatted(BRAND, BRAND_DARK, INK, escape(headline), contentHtml, MUTED);
    }

    /** A paragraph row. */
    public static String paragraph(String text) {
        return """
                <tr><td style="padding:6px 32px 0;">
                  <p style="font-size:15px;color:%s;line-height:1.6;margin:8px 0 0;">%s</p>
                </td></tr>
                """.formatted(INK, escape(text));
    }

    /**
     * A call-to-action button, always followed by the raw URL: plenty of clients and corporate mail
     * gateways mangle or strip buttons, and a link the reader can copy is the difference between a
     * working invitation and a support ticket.
     */
    public static String button(String label, String url) {
        String safeUrl = escape(url);
        return """
                <tr><td style="padding:22px 32px 0;">
                  <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                    <td style="background:%s;border-radius:10px;">
                      <a href="%s" style="display:inline-block;padding:12px 22px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">%s</a>
                    </td>
                  </tr></table>
                </td></tr>
                <tr><td style="padding:14px 32px 0;">
                  <div style="font-size:12px;color:%s;line-height:1.6;word-break:break-all;">
                    If the button doesn't work, paste this into your browser:<br/>
                    <span style="color:%s;">%s</span>
                  </div>
                </td></tr>
                """.formatted(BRAND, safeUrl, escape(label), MUTED, BRAND, safeUrl);
    }

    /** A muted note row (expiry, "ignore this if unexpected", …). */
    public static String note(String text) {
        return """
                <tr><td style="padding:16px 32px 0;">
                  <div style="font-size:12px;color:%s;line-height:1.6;">%s</div>
                </td></tr>
                """.formatted(MUTED, escape(text));
    }

    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
