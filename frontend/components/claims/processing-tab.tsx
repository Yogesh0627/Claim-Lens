"use client";

import { AlertTriangle, Copy, FileScan, ShieldAlert } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DataState, EmptyState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { useAsync } from "@/hooks/useAsync";
import { processingService } from "@/services/processingService";
import { EXIF_STATE_META, PROCESSING_STATUS_META, RISK_META, statusMeta } from "@/lib/enums";
import { formatDateTime } from "@/lib/dayjs";
import type {
  AnalysisResultResponse,
  ClaimProcessingResponse,
  OcrResultResponse,
} from "@/lib/types";

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
      <CardContent className="space-y-2">
        <div className="flex items-center gap-3">
          <span className="text-3xl font-semibold">{fraud.score ?? "—"}</span>
          {fraud.riskLevel ? <StatusBadge value={fraud.riskLevel} map={RISK_META} /> : null}
        </div>
        {fraud.explanation ? (
          <p className="text-muted-foreground text-sm whitespace-pre-wrap">{fraud.explanation}</p>
        ) : null}
      </CardContent>
    </Card>
  );
}

function AnalysisCard({ a }: { a: AnalysisResultResponse }) {
  return (
    <Card className="p-4">
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
      </div>
      {a.phash ? (
        <p className="text-muted-foreground mt-2 font-mono text-xs">pHash {a.phash}</p>
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
