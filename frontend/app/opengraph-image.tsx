import { ImageResponse } from "next/og";

/**
 * The social preview card (LinkedIn, X, WhatsApp, Slack).
 *
 * <p>Generated rather than a checked-in PNG so it can never drift from the tagline, and so there's
 * no binary to re-export whenever the wording changes. Deliberately uses no external fonts or
 * images: a remote fetch here would fail the build offline and slow every render.
 */
export const alt = "ClaimLens — Settle honest claims faster. Catch the ones that aren't.";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OpengraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          // Matches the app's neutral theme: --primary #171717 on --primary-foreground #fafafa.
          background: "linear-gradient(135deg, #171717 0%, #404040 100%)",
          padding: "72px 80px",
          fontFamily: "sans-serif",
        }}
      >
        {/* Brand mark + wordmark */}
        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              width: 72,
              height: 72,
              borderRadius: 18,
              background: "#fafafa",
            }}
          >
            <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#171717"
                 strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z" />
              <path d="m9 12 2 2 4-4" />
            </svg>
          </div>
          <div style={{ fontSize: 46, fontWeight: 700, color: "#fafafa", letterSpacing: "-0.02em" }}>
            ClaimLens
          </div>
        </div>

        {/* The tagline is the message — everything else is framing. */}
        <div style={{ display: "flex", flexDirection: "column", gap: 18 }}>
          <div
            style={{
              fontSize: 68,
              fontWeight: 700,
              color: "#fafafa",
              lineHeight: 1.15,
              letterSpacing: "-0.03em",
              maxWidth: 940,
            }}
          >
            Settle honest claims faster. Catch the ones that aren&apos;t.
          </div>
          <div style={{ fontSize: 30, color: "#a3a3a3", lineHeight: 1.4, maxWidth: 900 }}>
            Multi-tenant motor-insurance claims — OCR intake, image-fraud forensics, an explainable
            fraud engine and cited policy answers.
          </div>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 14, fontSize: 24, color: "#a3a3a3" }}>
          <span>Spring Boot 4</span><span>·</span>
          <span>Next.js 16</span><span>·</span>
          <span>PostgreSQL</span><span>·</span>
          <span>Python</span><span>·</span>
          <span>Gemini</span>
        </div>
      </div>
    ),
    size,
  );
}
