"use client";

import { Download, Eye, FileText } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { IconButton } from "@/components/icon-button";
import { DataState, EmptyState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { useAsync } from "@/hooks/useAsync";
import { portalService } from "@/services/portalService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { formatCurrency } from "@/lib/format";
import { formatDate } from "@/lib/dayjs";
import { isApiError } from "@/lib/http";
import type { PolicyResponse } from "@/lib/types";

export default function PortalPoliciesPage() {
  const { data, loading, error } = useAsync(() => portalService.myPolicies(), []);

  return (
    <>
      <PageHeader title="My policies" description="The cover you hold with us." />

      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={<EmptyState title="No policies" description="No policies are linked to your account." />}
      >
        {(policies) => (
          <div className="grid gap-4 sm:grid-cols-2">
            {policies.map((p) => (
              <PolicyCard key={p.id} policy={p} />
            ))}
          </div>
        )}
      </DataState>
    </>
  );
}

function PolicyCard({ policy: p }: { policy: PolicyResponse }) {
  // Documents live on the product version this policy is pinned to, resolved server-side.
  const docs = useAsync(() => portalService.policyDocuments(p.id), [p.id]);

  const view = async (documentId: number) => {
    try {
      const url = await portalService.policyDocumentUrl(p.id, documentId);
      window.open(url, "_blank", "noopener");
    } catch (e) {
      toast.error(isApiError(e) ? e.message : "Could not open document");
    }
  };

  const download = async (documentId: number, fileName: string) => {
    try {
      const url = await portalService.policyDocumentUrl(p.id, documentId);
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
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <CardTitle className="text-base">{p.policyNumber}</CardTitle>
        <StatusBadge value={p.status} map={GENERIC_STATUS_META} />
      </CardHeader>
      <CardContent className="text-sm">
        <dl className="grid grid-cols-2 gap-y-2">
          <dt className="text-muted-foreground">Vehicle</dt>
          <dd className="text-right font-medium">{p.vehicle?.registrationNumber ?? "—"}</dd>
          <dt className="text-muted-foreground">Make / model</dt>
          <dd className="text-right font-medium">
            {p.vehicle ? `${p.vehicle.make} ${p.vehicle.model}` : "—"}
          </dd>
          <dt className="text-muted-foreground">Sum insured</dt>
          <dd className="text-right font-medium">
            {formatCurrency(p.sumInsured, p.currency ?? "INR")}
          </dd>
          <dt className="text-muted-foreground">Cover</dt>
          <dd className="text-right font-medium">
            {formatDate(p.effectiveFrom)} – {formatDate(p.effectiveTo)}
          </dd>
        </dl>

        {docs.data && docs.data.length > 0 ? (
          <div className="mt-4 space-y-2 border-t pt-3">
            <p className="text-muted-foreground text-xs font-medium">Policy documents</p>
            {docs.data.map((d) => (
              <div key={d.id} className="flex items-center justify-between gap-3">
                <div className="flex min-w-0 items-center gap-2">
                  <FileText className="text-muted-foreground h-4 w-4 shrink-0" />
                  <span className="truncate text-sm">{d.fileName}</span>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <IconButton label="View" onClick={() => view(d.id)}>
                    <Eye className="h-4 w-4" />
                  </IconButton>
                  <IconButton label="Download" onClick={() => download(d.id, d.fileName)}>
                    <Download className="h-4 w-4" />
                  </IconButton>
                </div>
              </div>
            ))}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
