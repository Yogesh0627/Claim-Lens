"use client";

import { useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Download, FileText, Send, Upload } from "lucide-react";
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

/** Plain-language line under the status, so a customer knows what's happening without insurance jargon. */
const STATUS_HELP: Record<string, string> = {
  DRAFT: "Your claim is a draft. Upload any documents, then submit it to us.",
  AWAITING_ANALYSIS: "We've received your claim and are reviewing the documents.",
  AWAITING_ASSIGNMENT: "Your claim is being assigned to an investigator.",
  AWAITING_ACCEPTANCE: "An investigator is picking up your claim.",
  UNDER_INVESTIGATION: "An investigator is reviewing your claim.",
  WAITING_FOR_CUSTOMER: "We need more information from you — please check your messages.",
  APPROVED: "Good news — your claim has been approved.",
  REJECTED: "Your claim was not approved. Contact support if you have questions.",
  CLOSED: "This claim is closed.",
};

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4 py-2">
      <span className="text-muted-foreground text-sm">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  );
}

export default function PortalClaimDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const claimId = Number(params.id);

  const claim = useAsync(() => portalService.myClaim(claimId), [claimId]);
  const documents = useAsync(() => portalService.listDocuments(claimId), [claimId]);

  const [docType, setDocType] = useState<string>(DOCUMENT_TYPES[0]);
  const fileInput = useRef<HTMLInputElement>(null);
  const isDraft = claim.data?.status === "DRAFT";

  const upload = useMutation(
    (file: File) => portalService.uploadDocument(claimId, docType, file),
    { successMessage: "Document uploaded", onSuccess: () => documents.refetch() },
  );
  const submit = useMutation(() => portalService.submitClaim(claimId), {
    successMessage: "Claim submitted",
    onSuccess: () => claim.refetch(),
  });

  const onPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) upload.run(file);
    if (fileInput.current) fileInput.current.value = "";
  };

  const view = async (documentId: number) => {
    try {
      const url = await portalService.downloadUrl(claimId, documentId);
      window.open(url, "_blank", "noopener");
    } catch (e) {
      toast.error(isApiError(e) ? e.message : "Could not open document");
    }
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

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Claim details</CardTitle>
            </CardHeader>
            <CardContent className="divide-y">
              <Row label="Policy" value={claim.data.policyNumber ?? "—"} />
              <Row label="Vehicle" value={claim.data.vehicleRegistrationNumber ?? "—"} />
              <Row label="Incident date" value={formatDate(claim.data.incidentDate)} />
              <Row
                label="Amount"
                value={
                  claim.data.claimAmount != null
                    ? formatCurrency(claim.data.claimAmount, "INR")
                    : "—"
                }
              />
              {claim.data.submittedAt ? (
                <Row label="Submitted" value={formatDateTime(claim.data.submittedAt)} />
              ) : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Documents</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {isDraft ? (
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
                    variant="outline"
                    onClick={() => fileInput.current?.click()}
                    disabled={upload.loading}
                  >
                    <Upload className="mr-1 h-4 w-4" /> {upload.loading ? "Uploading…" : "Upload"}
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
                      <div
                        key={doc.id}
                        className="flex items-center justify-between gap-4 rounded-md border p-3"
                      >
                        <div className="flex min-w-0 items-center gap-3">
                          <FileText className="text-muted-foreground h-5 w-5 shrink-0" />
                          <div className="min-w-0">
                            <p className="truncate font-medium">{doc.fileName}</p>
                            <p className="text-muted-foreground text-xs">
                              {optionLabel(doc.documentType)} · {formatBytes(doc.sizeBytes)}
                            </p>
                          </div>
                        </div>
                        <IconButton label="View / download" onClick={() => view(doc.id)}>
                          <Download className="h-4 w-4" />
                        </IconButton>
                      </div>
                    ))}
                  </div>
                )}
              </DataState>
            </CardContent>
          </Card>
        </>
      )}
    </>
  );
}
