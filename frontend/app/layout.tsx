import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from "@/components/providers";
import { ServerStatus } from "@/components/server-status";
import { cn } from "@/lib/utils";

const geist = Geist({ subsets: ["latin"], variable: "--font-sans" });

const geistMono = Geist_Mono({
  variable: "--font-mono",
  subsets: ["latin"],
});

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://claimlens.app";
const DESCRIPTION =
  "ClaimLens is a multi-tenant SaaS for motor-insurance claims: OCR document intake, image-fraud " +
  "signals, an explainable fraud engine, auto-assignment, and AI policy answers — with strict " +
  "tenant isolation and a full audit trail.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "ClaimLens — Motor Insurance Claims, Automated",
    template: "%s · ClaimLens",
  },
  description: DESCRIPTION,
  applicationName: "ClaimLens",
  keywords: [
    "insurance claims software",
    "motor insurance",
    "claims automation",
    "insurance fraud detection",
    "OCR document processing",
    "multi-tenant SaaS",
    "claims management platform",
  ],
  authors: [{ name: "Yogesh Chauhan", url: "https://yogeshchauhan.dev" }],
  creator: "Yogesh Chauhan",
  openGraph: {
    type: "website",
    siteName: "ClaimLens",
    title: "ClaimLens — Motor Insurance Claims, Automated",
    description: DESCRIPTION,
    url: SITE_URL,
  },
  twitter: {
    card: "summary_large_image",
    title: "ClaimLens — Motor Insurance Claims, Automated",
    description: DESCRIPTION,
    creator: "@Yogesh0130",
  },
  robots: { index: true, follow: true },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      className={cn(geistMono.variable, "font-sans", geist.variable)}
    >
      <body className="bg-background text-foreground min-h-screen antialiased">
        <Providers>
          {children}
          <ServerStatus />
        </Providers>
      </body>
    </html>
  );
}
