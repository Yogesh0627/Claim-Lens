"use client";

import { useParams, useRouter } from "next/navigation";
import { AlertTriangle, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { LoadingRows, ErrorState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { ClaimActions } from "@/components/claims/claim-actions";
import { DocumentsTab } from "@/components/claims/documents-tab";
import { ProcessingTab } from "@/components/claims/processing-tab";
import { CoverageTab } from "@/components/claims/coverage-tab";
import { NotesTab } from "@/components/claims/notes-tab";
import { AuditTab } from "@/components/claims/audit-tab";
import { useAsync } from "@/hooks/useAsync";
import { useAuth } from "@/hooks/useAuth";
import { claimService } from "@/services/claimService";
import { CLAIM_STATUS_META } from "@/lib/enums";
import { formatCurrency } from "@/lib/format";
import { formatDate, formatDateTime } from "@/lib/dayjs";
import { PERMISSIONS } from "@/lib/permissions";
import type { ClaimResponse } from "@/lib/types";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4 py-2">
      <span className="text-muted-foreground text-sm">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  );
}

function Overview({ claim }: { claim: ClaimResponse }) {
  return (
    <div className="space-y-4">
      {claim.warnings.length > 0 ? (
        <Alert>
          <AlertTriangle className="h-4 w-4" />
          <AlertTitle>Soft validation signals</AlertTitle>
          <AlertDescription>
            <ul className="list-disc pl-4">
              {claim.warnings.map((w) => (
                <li key={w}>{w}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      ) : null}
      <Card>
        <CardContent className="divide-y pt-6">
          <DetailRow label="Claim number" value={claim.claimNumber} />
          <DetailRow
            label="Customer"
            value={
              claim.customerName
                ? `${claim.customerName}${claim.customerNumber ? ` · ${claim.customerNumber}` : ""}`
                : `#${claim.customerId}`
            }
          />
          <DetailRow
            label="Raised by"
            value={
              claim.raisedByName ? (
                <span className="inline-flex items-center gap-2">
                  {claim.raisedByName}
                  {claim.raisedByCode ? (
                    <span className="text-muted-foreground">· {claim.raisedByCode}</span>
                  ) : null}
                  {claim.raisedByStaff ? (
                    <span className="rounded bg-blue-100 px-1.5 py-0.5 text-[10px] font-medium text-blue-700 dark:bg-blue-950 dark:text-blue-300">
                      Staff
                    </span>
                  ) : (
                    <span className="text-muted-foreground rounded border px-1.5 py-0.5 text-[10px] font-medium">
                      Self-service
                    </span>
                  )}
                </span>
              ) : (
                "—"
              )
            }
          />
          <DetailRow
            label="Investigating officer"
            value={
              claim.investigatingOfficerName ? (
                <span className="inline-flex items-center gap-2">
                  {claim.investigatingOfficerName}
                  {claim.investigatingOfficerCode ? (
                    <span className="text-muted-foreground">· {claim.investigatingOfficerCode}</span>
                  ) : null}
                </span>
              ) : (
                <span className="text-muted-foreground">Not yet assigned</span>
              )
            }
          />
          <DetailRow label="Policy number" value={claim.policyNumber ?? "—"} />
          <DetailRow label="Vehicle" value={claim.vehicleRegistrationNumber ?? "—"} />
          <DetailRow label="Incident date" value={formatDate(claim.incidentDate)} />
          <DetailRow label="Claim amount" value={formatCurrency(claim.claimAmount)} />
          <DetailRow label="Product" value={claim.productName ?? "—"} />
          <DetailRow
            label="Product version"
            value={claim.productVersionNumber != null ? `v${claim.productVersionNumber}` : "—"}
          />
          <DetailRow
            label="Submitted"
            value={claim.submittedAt ? formatDateTime(claim.submittedAt) : "Not submitted"}
          />
        </CardContent>
      </Card>
    </div>
  );
}

export default function ClaimDetailPage() {
  const params = useParams<{ id: string }>();
  const claimId = Number(params.id);
  const router = useRouter();
  const { has } = useAuth();
  const {
    data: claim,
    loading,
    error,
    refetch,
  } = useAsync(() => claimService.get(claimId), [claimId]);

  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        className="-ml-2 w-fit"
        onClick={() => router.push("/claims")}
      >
        <ArrowLeft className="mr-1 h-4 w-4" /> Claims
      </Button>

      {loading ? (
        <LoadingRows />
      ) : error || !claim ? (
        <ErrorState message={error ?? "Claim not found"} />
      ) : (
        <>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-semibold tracking-tight">{claim.claimNumber}</h1>
              <StatusBadge value={claim.status} map={CLAIM_STATUS_META} />
            </div>
            <ClaimActions claim={claim} onChanged={refetch} />
          </div>

          <Tabs defaultValue="overview">
            <div className="-mx-1 overflow-x-auto px-1">
              <TabsList className="w-max">
                <TabsTrigger value="overview">Overview</TabsTrigger>
                <TabsTrigger value="documents">Documents</TabsTrigger>
                <TabsTrigger value="processing">Processing</TabsTrigger>
                {has(PERMISSIONS.COVERAGE_READ) ? (
                  <TabsTrigger value="coverage">Coverage</TabsTrigger>
                ) : null}
                <TabsTrigger value="notes">Investigation</TabsTrigger>
                {has(PERMISSIONS.AUDIT_READ) ? (
                  <TabsTrigger value="audit">Audit</TabsTrigger>
                ) : null}
              </TabsList>
            </div>
            <TabsContent value="overview" className="mt-4">
              <Overview claim={claim} />
            </TabsContent>
            <TabsContent value="documents" className="mt-4">
              <DocumentsTab claimId={claimId} />
            </TabsContent>
            <TabsContent value="processing" className="mt-4">
              <ProcessingTab claimId={claimId} />
            </TabsContent>
            {has(PERMISSIONS.COVERAGE_READ) ? (
              <TabsContent value="coverage" className="mt-4">
                <CoverageTab claimId={claimId} />
              </TabsContent>
            ) : null}
            <TabsContent value="notes" className="mt-4">
              <NotesTab claimId={claimId} />
            </TabsContent>
            {has(PERMISSIONS.AUDIT_READ) ? (
              <TabsContent value="audit" className="mt-4">
                <AuditTab claimId={claimId} />
              </TabsContent>
            ) : null}
          </Tabs>
        </>
      )}
    </>
  );
}
