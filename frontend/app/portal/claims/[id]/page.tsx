"use client";

import { useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft,
  Clock,
  Download,
  Eye,
  FileText,
  History,
  MessageSquareWarning,
  Send,
  Upload,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { LoadingRows, ErrorState, DataState, EmptyState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { IconButton } from "@/components/icon-button";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { portalService } from "@/services/portalService";
import { CLAIM_STATUS_META, DOCUMENT_TYPES, optionLabel } from "@/lib/enums";
import { formatCurrency } from "@/lib/format";
import { formatBytes } from "@/lib/format";
import { formatDate, formatDateTime } from "@/lib/dayjs";
import { isApiError } from "@/lib/http";
import type { DocumentResponse } from "@/lib/types";

/** Plain-language line under the status, so a customer knows what's happening without insurance jargon. */
const STATUS_HELP: Record<string, string> = {
  DRAFT: "Your claim is a draft. Upload any documents, then submit it to us.",
  AWAITING_ANALYSIS: "We've received your claim and are reviewing the documents.",
  AWAITING_ASSIGNMENT: "Your claim is being assigned to an investigator.",
  AWAITING_ACCEPTANCE: "An investigator is picking up your claim.",
  UNDER_INVESTIGATION: "An investigator is reviewing your claim.",
  WAITING_FOR_CUSTOMER: "We need more information from you — see the request below and upload the document.",
  APPROVED: "Good news — your claim has been approved.",
  REJECTED: "Your claim was not approved. Contact support if you have questions.",
  CLOSED: "This claim is closed.",
};

export default function PortalClaimDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const claimId = Number(params.id);

  const claim = useAsync(() => portalService.myClaim(claimId), [claimId]);
  const documents = useAsync(() => portalService.listDocuments(claimId), [claimId]);
  const timeline = useAsync(() => portalService.claimTimeline(claimId), [claimId]);

  const [docType, setDocType] = useState<string>(DOCUMENT_TYPES[0]);
  const fileInput = useRef<HTMLInputElement>(null);
  const status = claim.data?.status;
  const isDraft = status === "DRAFT";
  const awaitingCustomer = status === "WAITING_FOR_CUSTOMER";
  // A customer can add documents while drafting, or when we've asked them for more (the backend
  // re-opens the claim on that upload). Both states accept files; other states are read-only.
  const canUpload = isDraft || awaitingCustomer;

  // The investigator's request message is carried on the transition into WAITING_FOR_CUSTOMER.
  const requestMessage = awaitingCustomer
    ? [...(timeline.data ?? [])]
        .reverse()
        .find((e) => e.toStatus === "WAITING_FOR_CUSTOMER" && e.note?.startsWith("Information requested: "))
        ?.note?.replace("Information requested: ", "") ?? null
    : null;

  const upload = useMutation(
    (file: File) => portalService.uploadDocument(claimId, docType, file),
    {
      successMessage: awaitingCustomer ? "Response sent" : "Document uploaded",
      onSuccess: () => {
        documents.refetch();
        // Answering a request re-opens the claim server-side — refresh status + timeline too.
        claim.refetch();
        timeline.refetch();
      },
    },
  );
  const submit = useMutation(() => portalService.submitClaim(claimId), {
    successMessage: "Claim submitted",
    onSuccess: () => {
      claim.refetch();
      timeline.refetch();
    },
  });

  const onPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) upload.run(file);
    if (fileInput.current) fileInput.current.value = "";
  };

  return (
    <>
      <Button variant="ghost" size="sm" className="-ml-2 w-fit" onClick={() => router.push("/portal")}>
        <ArrowLeft className="mr-1 h-4 w-4" /> My claims
      </Button>

      {claim.loading ? (
        <LoadingRows />
      ) : claim.error || !claim.data ? (
        <ErrorState message={claim.error ?? "Claim not found"} />
      ) : (
        <>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">{claim.data.claimNumber}</h1>
            <StatusBadge value={claim.data.status} map={CLAIM_STATUS_META} />
            {isDraft ? (
              <Button
                size="sm"
                className="ml-auto"
                onClick={() => submit.run()}
                disabled={submit.loading}
              >
                <Send className="mr-1 h-4 w-4" /> {submit.loading ? "Submitting…" : "Submit claim"}
              </Button>
            ) : null}
          </div>
          <p className="text-muted-foreground text-sm">
            {STATUS_HELP[claim.data.status] ?? "We're processing your claim."}
          </p>

          {awaitingCustomer ? (
            <Card className="border-amber-500/50 bg-amber-50 dark:bg-amber-950/20">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base text-amber-900 dark:text-amber-200">
                  <MessageSquareWarning className="h-4 w-4" /> We need more information
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-1 text-sm">
                {requestMessage ? (
                  <p className="text-amber-900 dark:text-amber-100">“{requestMessage}”</p>
                ) : (
                  <p className="text-muted-foreground">
                    Your investigator has asked for more information on this claim.
                  </p>
                )}
                <p className="text-muted-foreground">
                  Upload the requested document below — we&apos;ll continue reviewing as soon as you do.
                </p>
              </CardContent>
            </Card>
          ) : null}

          <div className="grid gap-6 lg:grid-cols-3">
            {/* Left: the claim itself — compact details, then its documents. */}
            <div className="space-y-6 lg:col-span-2">
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-base">Claim details</CardTitle>
                </CardHeader>
                <CardContent>
                  <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                    <dt className="text-muted-foreground">Policy</dt>
                    <dd className="text-right font-medium">{claim.data.policyNumber ?? "—"}</dd>
                    <dt className="text-muted-foreground">Vehicle</dt>
                    <dd className="text-right font-medium">
                      {claim.data.vehicleRegistrationNumber ?? "—"}
                    </dd>
                    <dt className="text-muted-foreground">Incident date</dt>
                    <dd className="text-right font-medium">{formatDate(claim.data.incidentDate)}</dd>
                    <dt className="text-muted-foreground">Amount</dt>
                    <dd className="text-right font-medium">
                      {claim.data.claimAmount != null
                        ? formatCurrency(claim.data.claimAmount, "INR")
                        : "—"}
                    </dd>
                    {claim.data.submittedAt ? (
                      <>
                        <dt className="text-muted-foreground">Submitted</dt>
                        <dd className="text-right font-medium">
                          {formatDateTime(claim.data.submittedAt)}
                        </dd>
                      </>
                    ) : null}
                  </dl>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Documents</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {canUpload ? (
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                      <div className="grid flex-1 gap-2">
                        <Label>Document type</Label>
                        <Select value={docType} onValueChange={setDocType}>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {DOCUMENT_TYPES.map((t) => (
                              <SelectItem key={t} value={t}>
                                {optionLabel(t)}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      <input ref={fileInput} type="file" className="hidden" onChange={onPick} />
                      <Button
                        variant={awaitingCustomer ? "default" : "outline"}
                        onClick={() => fileInput.current?.click()}
                        disabled={upload.loading}
                      >
                        <Upload className="mr-1 h-4 w-4" />{" "}
                        {upload.loading
                          ? "Uploading…"
                          : awaitingCustomer
                            ? "Upload response"
                            : "Upload"}
                      </Button>
                    </div>
                  ) : null}

                  <DataState
                    loading={documents.loading}
                    error={documents.error}
                    data={documents.data}
                    emptyWhen={(d) => d.length === 0}
                    empty={
                      <p className="text-muted-foreground text-sm">No documents on this claim yet.</p>
                    }
                  >
                    {(docs) => (
                      <div className="space-y-2">
                        {docs.map((doc) => (
                          <DocumentRow key={doc.id} claimId={claimId} doc={doc} />
                        ))}
                      </div>
                    )}
                  </DataState>
                </CardContent>
              </Card>
            </div>

            {/* Right: the journey. Sticky on large screens so it stays in view while scrolling. */}
            <div className="lg:col-span-1">
              <Card className="lg:sticky lg:top-20">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-base">
                    <Clock className="h-4 w-4" /> Progress
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <DataState
                    loading={timeline.loading}
                    error={timeline.error}
                    data={timeline.data}
                    emptyWhen={(t) => t.length === 0}
                    empty={
                      <p className="text-muted-foreground text-sm">No updates on this claim yet.</p>
                    }
                  >
                    {(entries) => (
                      <ol className="relative space-y-4 border-l pl-6">
                        {[...entries].reverse().map((e, i) => (
                          <li key={i} className="relative">
                            <span className="bg-primary absolute left-[calc(-1.5rem-0.5px)] top-1.5 h-2.5 w-2.5 -translate-x-1/2 rounded-full" />
                            <p className="text-sm font-medium">
                              {CLAIM_STATUS_META[e.toStatus]?.label ?? optionLabel(e.toStatus)}
                            </p>
                            {e.note ? (
                              <p className="text-muted-foreground text-sm">{e.note}</p>
                            ) : null}
                            <p className="text-muted-foreground text-xs">{formatDateTime(e.at)}</p>
                          </li>
                        ))}
                      </ol>
                    )}
                  </DataState>
                </CardContent>
              </Card>
            </div>
          </div>
        </>
      )}
    </>
  );
}

/** View opens in a new tab; save downloads to disk via an anchor carrying the filename. */
async function openInTab(loadUrl: () => Promise<string>) {
  try {
    window.open(await loadUrl(), "_blank", "noopener");
  } catch (e) {
    toast.error(isApiError(e) ? e.message : "Could not open document");
  }
}

async function saveFile(loadUrl: () => Promise<string>, fileName: string) {
  try {
    const url = await loadUrl();
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (e) {
    toast.error(isApiError(e) ? e.message : "Could not download document");
  }
}

/**
 * One document row — its type, size, upload time and current version, with View/Download for the
 * live file. When a document has been revised (more than one version), a history toggle reveals every
 * version with its own date and download.
 */
function DocumentRow({ claimId, doc }: { claimId: number; doc: DocumentResponse }) {
  const [showHistory, setShowHistory] = useState(false);
  const versions = useAsync(() => portalService.documentVersions(claimId, doc.id), [claimId, doc.id]);
  const list = versions.data ?? [];
  const current = list.find((v) => v.current);
  const hasHistory = list.length > 1;
  // Date of the file actually shown (the current version); falls back to the document's own date.
  const uploadedAt = current?.createdAt ?? doc.createdAt;

  return (
    <div className="rounded-md border p-3">
      <div className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          <FileText className="text-muted-foreground h-5 w-5 shrink-0" />
          <div className="min-w-0">
            <p className="truncate font-medium">{doc.fileName}</p>
            <p className="text-muted-foreground text-xs">
              {optionLabel(doc.documentType)} · {formatBytes(doc.sizeBytes)}
              {current != null ? ` · v${current.versionNumber}` : ""}
            </p>
            <p className="text-muted-foreground text-xs">Uploaded {formatDateTime(uploadedAt)}</p>
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          {hasHistory ? (
            <IconButton
              label={showHistory ? "Hide history" : "Version history"}
              onClick={() => setShowHistory((s) => !s)}
            >
              <History className="h-4 w-4" />
            </IconButton>
          ) : null}
          <IconButton
            label="View"
            onClick={() => openInTab(() => portalService.downloadUrl(claimId, doc.id))}
          >
            <Eye className="h-4 w-4" />
          </IconButton>
          <IconButton
            label="Download"
            onClick={() => saveFile(() => portalService.downloadUrl(claimId, doc.id), doc.fileName)}
          >
            <Download className="h-4 w-4" />
          </IconButton>
        </div>
      </div>

      {showHistory && hasHistory ? (
        <div className="mt-3 space-y-2 border-t pt-3">
          <p className="text-muted-foreground text-xs font-medium">Version history</p>
          {list.map((v) => (
            <div key={v.id} className="flex items-center justify-between gap-3 text-sm">
              <span className="flex min-w-0 items-center gap-2">
                <span className="text-muted-foreground w-7 shrink-0 font-mono">v{v.versionNumber}</span>
                <span className="truncate">{v.fileName}</span>
                {v.current ? (
                  <span className="bg-primary/10 text-primary rounded px-1.5 py-0.5 text-[10px] font-medium">
                    current
                  </span>
                ) : null}
              </span>
              <span className="flex shrink-0 items-center gap-1">
                <span className="text-muted-foreground mr-1 text-xs">
                  {formatDateTime(v.createdAt)}
                </span>
                <IconButton
                  label={`View v${v.versionNumber}`}
                  onClick={() =>
                    openInTab(() => portalService.documentVersionUrl(claimId, doc.id, v.id))
                  }
                >
                  <Eye className="h-4 w-4" />
                </IconButton>
                <IconButton
                  label={`Download v${v.versionNumber}`}
                  onClick={() =>
                    saveFile(
                      () => portalService.documentVersionUrl(claimId, doc.id, v.id),
                      v.fileName,
                    )
                  }
                >
                  <Download className="h-4 w-4" />
                </IconButton>
              </span>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}
