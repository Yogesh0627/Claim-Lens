"use client";

import { AlertTriangle, Copy, FileScan, ShieldAlert } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DataState, EmptyState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { useAsync } from "@/hooks/useAsync";
import { processingService } from "@/services/processingService";
import {
  EXIF_STATE_META,
  PROCESSING_STATUS_META,
  RISK_META,
  optionLabel,
  statusMeta,
} from "@/lib/enums";
import { formatDateTime } from "@/lib/dayjs";
import type {
  AnalysisResultResponse,
  ClaimProcessingResponse,
  OcrResultResponse,
} from "@/lib/types";

/** Friendly, plain-language names for the fraud rule codes the engine emits. */
const FRAUD_RULE_LABELS: Record<string, string> = {
  AMOUNT_OVER_SUM_INSURED: "Claim amount exceeds the sum insured",
  EARLY_CLAIM: "Claim filed soon after the policy started",
  DUPLICATE_IMAGE: "A photo matches one used on another claim",
  SYNTHETIC_IMAGE: "A photo may be AI-generated / synthetic",
  EXIF_INCONSISTENT: "Photo metadata is inconsistent with the incident",
  REPEAT_CLAIM_ON_VEHICLE: "Repeat claim on the same vehicle",
  CLAIM_AMOUNT_ANOMALY: "Claim amount is unusually high vs. baseline",
};

/** What each EXIF state means for a reviewer — UNKNOWN is explicitly NOT suspicious. */
const EXIF_MEANING: Record<string, string> = {
  UNKNOWN: "Metadata stripped — normal for photos shared via WhatsApp/social, not a fraud signal.",
  INCONSISTENT: "Capture time or location doesn't match the reported incident.",
  CONSISTENT: "Metadata is consistent with the reported incident.",
};

/**
 * Turns the engine's explanation into a per-signal list. Handles both the machine format
 * ("DUPLICATE_IMAGE (+40); SYNTHETIC_IMAGE (+25)") and seeded narrative sentences, so the breakdown
 * renders whatever produced the score.
 */
function parseSignals(explanation?: string | null): { label: string; weight: number | null }[] {
  if (!explanation || explanation.toLowerCase().includes("no fraud signal")) return [];
  // Split on ';' only — both the machine format and the seeded narratives use it as the separator,
  // whereas '.' appears inside abbreviations like "vs." and would split mid-phrase.
  return explanation
    .split(";")
    .map((s) => s.trim().replace(/\.$/, "").trim())
    .filter(Boolean)
    .map((part) => {
      const m = part.match(/^([A-Z_]+)\s*\(\+(\d+)\)$/);
      if (m) return { label: FRAUD_RULE_LABELS[m[1]] ?? optionLabel(m[1]), weight: Number(m[2]) };
      return { label: part.charAt(0).toUpperCase() + part.slice(1), weight: null };
    });
}

function PipelineState({ data }: { data: ClaimProcessingResponse }) {
  const s = data.state;
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium">Pipeline</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-wrap gap-4 text-sm">
        <div className="flex items-center gap-2">
          <span className="text-muted-foreground">OCR</span>
          <StatusBadge value={s?.ocrStatus ?? "NOT_STARTED"} map={PROCESSING_STATUS_META} />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-muted-foreground">Analysis</span>
          <StatusBadge value={s?.analysisStatus ?? "NOT_STARTED"} map={PROCESSING_STATUS_META} />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-muted-foreground">Fraud</span>
          <StatusBadge value={s?.fraudStatus ?? "NOT_STARTED"} map={PROCESSING_STATUS_META} />
        </div>
      </CardContent>
    </Card>
  );
}

function FraudCard({ fraud }: { fraud: NonNullable<ClaimProcessingResponse["fraud"]> }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-sm font-medium">
          <ShieldAlert className="h-4 w-4" /> Fraud score
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex items-center gap-3">
          <span className="text-3xl font-semibold">{fraud.score ?? "—"}</span>
          {fraud.riskLevel ? <StatusBadge value={fraud.riskLevel} map={RISK_META} /> : null}
          <span className="text-muted-foreground text-xs">out of 100</span>
        </div>
        {(() => {
          const signals = parseSignals(fraud.explanation);
          if (signals.length === 0) {
            return <p className="text-muted-foreground text-sm">No fraud signals triggered.</p>;
          }
          return (
            <div className="space-y-1.5">
              <p className="text-muted-foreground text-[11px] font-medium uppercase">
                Why this score
              </p>
              {signals.map((s, i) => (
                <div
                  key={i}
                  className="flex items-start justify-between gap-3 rounded-md border p-2 text-sm"
                >
                  <span className="flex items-start gap-2">
                    <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-500" />
                    {s.label}
                  </span>
                  {s.weight != null ? (
                    <span className="shrink-0 rounded bg-amber-100 px-1.5 py-0.5 text-xs font-semibold text-amber-800 dark:bg-amber-950 dark:text-amber-300">
                      +{s.weight}
                    </span>
                  ) : null}
                </div>
              ))}
            </div>
          );
        })()}
      </CardContent>
    </Card>
  );
}

function AnalysisCard({ a }: { a: AnalysisResultResponse }) {
  const clean = !a.syntheticSignal && !a.duplicateOfClaimId && a.exifState !== "INCONSISTENT";
  return (
    <Card className="space-y-2 p-4">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm font-medium">Document #{a.documentId}</span>
        {a.exifState ? <StatusBadge value={a.exifState} map={EXIF_STATE_META} /> : null}
        {a.syntheticSignal ? (
          <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-950 dark:text-amber-300">
            <AlertTriangle className="h-3 w-3" /> Possible synthetic
            {a.syntheticScore != null ? ` (${a.syntheticScore.toFixed(2)})` : ""}
          </span>
        ) : null}
        {a.duplicateOfClaimId ? (
          <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700 dark:bg-red-950 dark:text-red-300">
            <Copy className="h-3 w-3" /> Duplicate of claim #{a.duplicateOfClaimId}
          </span>
        ) : null}
        {clean ? <span className="text-muted-foreground text-xs">No image red flags</span> : null}
      </div>
      {a.exifState && EXIF_MEANING[a.exifState] ? (
        <p className="text-muted-foreground text-xs">EXIF: {EXIF_MEANING[a.exifState]}</p>
      ) : null}
      {a.duplicateOfClaimId ? (
        <p className="text-muted-foreground text-xs">
          This image is a near-identical match (perceptual hash) to one already on claim #
          {a.duplicateOfClaimId} — a strong reuse signal.
        </p>
      ) : null}
      {a.phash ? (
        <p className="text-muted-foreground font-mono text-xs">pHash {a.phash}</p>
      ) : null}
    </Card>
  );
}

function OcrCard({ o }: { o: OcrResultResponse }) {
  return (
    <Card className="p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="text-sm font-medium">Document #{o.documentId}</span>
        <span className="text-muted-foreground text-xs">
          {o.engine}
          {o.confidence != null ? ` · ${Number(o.confidence).toFixed(0)}% confidence` : ""}
        </span>
      </div>
      {o.registrationNumbers || o.policyNumbers ? (
        <div className="mt-2 flex flex-wrap gap-2 text-xs">
          {o.registrationNumbers
            ? o.registrationNumbers.split(",").map((r) => (
                <span key={r} className="bg-muted rounded px-2 py-0.5 font-mono">
                  reg: {r}
                </span>
              ))
            : null}
          {o.policyNumbers
            ? o.policyNumbers.split(",").map((p) => (
                <span key={p} className="bg-muted rounded px-2 py-0.5 font-mono">
                  policy: {p}
                </span>
              ))
            : null}
        </div>
      ) : null}
      {o.extractedText ? (
        <details className="mt-2">
          <summary className="text-muted-foreground cursor-pointer text-xs">Extracted text</summary>
          <pre className="bg-muted mt-2 max-h-64 overflow-auto rounded p-2 text-xs whitespace-pre-wrap">
            {o.extractedText}
          </pre>
        </details>
      ) : null}
    </Card>
  );
}

export function ProcessingTab({ claimId }: { claimId: number }) {
  const { data, loading, error } = useAsync(() => processingService.get(claimId), [claimId]);

  return (
    <DataState loading={loading} error={error} data={data}>
      {(d) => {
        const nothing =
          d.ocrResults.length === 0 && d.analysisResults.length === 0 && !d.fraud && !d.state;
        return (
          <div className="space-y-4">
            {d.state ? <PipelineState data={d} /> : null}
            {d.fraud ? <FraudCard fraud={d.fraud} /> : null}

            {d.analysisResults.length > 0 ? (
              <section className="space-y-2">
                <h3 className="flex items-center gap-2 text-sm font-semibold">
                  <FileScan className="h-4 w-4" /> Image analysis
                </h3>
                {d.analysisResults.map((a) => (
                  <AnalysisCard key={a.id} a={a} />
                ))}
              </section>
            ) : null}

            {d.ocrResults.length > 0 ? (
              <section className="space-y-2">
                <h3 className="text-sm font-semibold">OCR results</h3>
                {d.ocrResults.map((o) => (
                  <OcrCard key={o.id} o={o} />
                ))}
              </section>
            ) : null}

            {nothing ? (
              <EmptyState
                title="No processing results yet"
                description="OCR and image analysis run after a claim is submitted, once the services are enabled."
              />
            ) : null}
          </div>
        );
      }}
    </DataState>
  );
}
