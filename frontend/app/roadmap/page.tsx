import type { Metadata } from "next";
import { Check, Circle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = {
  title: "Roadmap",
  description:
    "What's shipped in ClaimLens and what's next — multi-tenant claims, OCR, image fraud forensics, an explainable fraud engine, AI policy answers, and a customer portal, plus the deliberately-deferred Phase 2.",
  robots: { index: true, follow: true },
};

const SHIPPED: { group: string; items: string[] }[] = [
  {
    group: "Foundation & security",
    items: [
      "Multi-tenant isolation enforced in the database (Hibernate @TenantId), not by convention",
      "JWT auth with server-side, per-request RBAC (permissions resolved from roles, not the token)",
      "Brute-force protection — sign-in is rate limited per client, shared across instances via Redis",
      "Append-only audit trail across state changes",
      "Refresh-token rotation with theft detection",
    ],
  },
  {
    group: "Claim lifecycle",
    items: [
      "Three-step intake — create draft, upload documents, submit — resumable by design",
      "Hard vs soft validation: reject only what's certainly invalid; everything else is a fraud signal",
      "Policy version pinning — a claim is judged against the terms the customer actually bought",
      "Two-way information requests — an investigator asks for a document, the customer uploads it from the portal, and the claim automatically re-opens, reprocesses and re-notifies",
      "Full lifecycle through assignment, investigation and an approve/reject decision",
    ],
  },
  {
    group: "Document AI (OCR)",
    items: [
      "Google Cloud Vision OCR, swappable behind an interface (Tesseract offline)",
      "Per-document processing with structured field extraction (registration & policy numbers)",
      "Document versioning — re-uploads keep the full history",
    ],
  },
  {
    group: "Image fraud forensics",
    items: [
      "Perceptual-hash duplicate detection across a tenant's claims (top motor-fraud pattern)",
      "ORB feature-matching for lightly-edited reuse",
      "Three-state EXIF — only genuine inconsistency counts; missing metadata is never suspicion",
      "Synthetic / AI-generated image soft signal",
    ],
  },
  {
    group: "Fraud engine",
    items: [
      "Config-driven rules engine with per-rule scoring — every score is explainable",
      "Risk levels (high / medium / low) surfaced to investigators",
      "Exactly-once fraud job that settles on terminal state, so one bad file can't hang a claim",
    ],
  },
  {
    group: "AI policy intelligence (RAG)",
    items: [
      "Plain-language coverage answers grounded in the policy wording, with citations",
      "Retrieval scoped to the claim's pinned version and to the tenant (no cross-tenant leak)",
      "Gated pgvector + HNSW option for scale; in-Java cosine everywhere else",
      "Graceful degradation — falls back to an offline answer if the AI provider is unavailable",
    ],
  },
  {
    group: "Portals",
    items: [
      "Customer self-service portal — file, upload, submit and track your own claims",
      "Ownership scoping below the tenant, so one customer can never see another's claim",
      "Cross-tenant platform console — analytics, tenant lifecycle, and impersonation",
    ],
  },
  {
    group: "Performance & platform",
    items: [
      "Redis caching of role→permission resolution (Caffeine in dev, Upstash Redis in prod)",
      "Provider-agnostic cache — the same code runs on either backend, switched by profile",
      "A cache outage degrades to a database read; it can never take the service down",
      "Pluggable object storage — local filesystem in dev, S3-compatible (Cloudflare R2) in production",
      "Tuned HikariCP connection pool sized for a managed Postgres (Neon)",
      "Containerised end to end: Docker images for all three services plus a Render blueprint",
    ],
  },
  {
    group: "Product & experience",
    items: [
      "Branded HTML claim emails with an attached PDF report — stats, decision, investigator, and the uploaded photos",
      "Email via a swappable sender (Resend in production; logs offline)",
      "One-click demo logins for every role; responsive UI with light/dark themes",
      "MDX blog and this roadmap",
    ],
  },
];

const NEXT: { title: string; detail: string }[] = [
  {
    title: "Observability",
    detail: "Metrics (Micrometer → Prometheus/Grafana) and structured JSON logs carrying trace / tenant / user context. The deployment path — Docker images, a Render blueprint, Neon, R2 storage and auth rate limiting — is already built.",
  },
  {
    title: "Vector search tuning",
    detail: "pgvector with an HNSW index already ships (opt-in). What's next is tuning that index and a backfill path so retrieval stays fast as the policy corpus grows.",
  },
  {
    title: "More claim types",
    detail: "Health and property lines on the same engine — the product is already claim-type-agnostic by construction.",
  },
  {
    title: "Real-time notifications",
    detail: "WebSocket / SSE push so investigators and customers see updates without refreshing.",
  },
  {
    title: "Outbox event worker",
    detail: "The outbox table ships today; the async publisher/consumer is the next step for reliable event delivery.",
  },
  {
    title: "Per-document reprocessing",
    detail: "The resubmission loop is live end-to-end; the next refinement is re-OCRing only the document that changed rather than re-running the whole claim's pipeline.",
  },
  {
    title: "Claimant mobile experience",
    detail: "A phone-first flow for filing a claim and uploading damage photos at the roadside.",
  },
  {
    title: "Analytics date ranges & export",
    detail: "Tenant and platform dashboards ship today, but report over all time. Next is filtering by date range and exporting to CSV / PDF.",
  },
];

export default function RoadmapPage() {
  return (
    <div className="mx-auto w-full max-w-5xl px-4 py-12 sm:px-6 sm:py-16">
      <div className="max-w-2xl">
        <p className="text-primary text-sm font-medium">Product roadmap</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
          What&apos;s built, and what&apos;s next
        </h1>
        <p className="text-muted-foreground mt-3 text-base sm:text-lg">
          ClaimLens is built on one principle: <span className="text-foreground">fast for honest
          claims, careful with suspicious ones, and accountable for every decision.</span> Here&apos;s
          what&apos;s live today and where it&apos;s headed.
        </p>
      </div>

      {/* legend */}
      <div className="text-muted-foreground mt-6 flex flex-wrap items-center gap-4 text-xs">
        <span className="flex items-center gap-1.5">
          <span className="flex h-4 w-4 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-600 dark:text-emerald-400">
            <Check className="h-3 w-3" />
          </span>
          Shipped &amp; verified
        </span>
        <span className="flex items-center gap-1.5">
          <Circle className="text-muted-foreground/60 h-3 w-3" /> Planned (Phase 2)
        </span>
      </div>

      {/* shipped */}
      <h2 className="mt-10 text-lg font-semibold tracking-tight">Shipped</h2>
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        {SHIPPED.map((section) => (
          <Card key={section.group}>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">{section.group}</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2.5">
                {section.items.map((item) => (
                  <li key={item} className="flex gap-2.5 text-sm">
                    <span className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-600 dark:text-emerald-400">
                      <Check className="h-3 w-3" />
                    </span>
                    <span className="text-muted-foreground">{item}</span>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* next */}
      <h2 className="mt-12 text-lg font-semibold tracking-tight">Next — Phase 2</h2>
      <p className="text-muted-foreground mt-1 text-sm">
        Deliberately deferred, not forgotten — each was a scope decision, sequenced behind a solid V1.
      </p>
      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        {NEXT.map((item) => (
          <div key={item.title} className="rounded-lg border border-dashed p-4">
            <div className="flex items-center gap-2">
              <Circle className="text-muted-foreground/60 h-3 w-3 shrink-0" />
              <h3 className="text-sm font-medium">{item.title}</h3>
            </div>
            <p className="text-muted-foreground mt-1.5 pl-5 text-sm">{item.detail}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
