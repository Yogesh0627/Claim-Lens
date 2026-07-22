"use client";

import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Ban } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { LoadingRows, ErrorState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { ConfirmDialog, useConfirm } from "@/components/confirm-dialog";
import { useAsync } from "@/hooks/useAsync";
import { policyService } from "@/services/policyService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { PERMISSIONS } from "@/lib/permissions";
import { formatCurrency } from "@/lib/format";
import { formatDate, formatDateTime } from "@/lib/dayjs";

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4 py-2">
      <span className="text-muted-foreground text-sm">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  );
}

export default function PolicyDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const {
    data: policy,
    loading,
    error,
    refetch,
  } = useAsync(() => policyService.get(Number(params.id)), [params.id]);
  const cancelConfirm = useConfirm<number>();

  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        className="-ml-2 w-fit"
        onClick={() => router.push("/policies")}
      >
        <ArrowLeft className="mr-1 h-4 w-4" /> Policies
      </Button>

      {loading ? (
        <LoadingRows />
      ) : error || !policy ? (
        <ErrorState message={error ?? "Policy not found"} />
      ) : (
        <>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">{policy.policyNumber}</h1>
            <StatusBadge value={policy.status} map={GENERIC_STATUS_META} />
            {policy.status !== "CANCELLED" ? (
              <Can permission={PERMISSIONS.POLICY_WRITE}>
                <Button
                  variant="outline"
                  size="sm"
                  className="ml-auto"
                  onClick={() => cancelConfirm.ask(policy.id)}
                >
                  <Ban className="mr-1 h-4 w-4" /> Cancel policy
                </Button>
              </Can>
            ) : null}
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Policy</CardTitle>
              </CardHeader>
              <CardContent className="divide-y">
                <Row label="Effective from" value={formatDate(policy.effectiveFrom)} />
                <Row label="Effective to" value={formatDate(policy.effectiveTo)} />
                <Row
                  label="Sum insured"
                  value={formatCurrency(policy.sumInsured, policy.currency ?? "INR")}
                />
                <Row
                  label="Deductible"
                  value={formatCurrency(policy.deductible, policy.currency ?? "INR")}
                />
                <Row
                  label="Premium"
                  value={formatCurrency(policy.premiumAmount, policy.currency ?? "INR")}
                />
                <Row label="Product version" value={policy.insuranceProductVersionId} />
                <Row label="Issued" value={formatDateTime(policy.issuedAt)} />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Insured vehicle</CardTitle>
              </CardHeader>
              <CardContent className="divide-y">
                <Row label="Registration" value={policy.vehicle?.registrationNumber ?? "—"} />
                <Row label="Make" value={policy.vehicle?.make ?? "—"} />
                <Row label="Model" value={policy.vehicle?.model ?? "—"} />
                <Row label="Variant" value={policy.vehicle?.variant ?? "—"} />
                <Row label="Year" value={policy.vehicle?.manufactureYear ?? "—"} />
                <Row label="Chassis" value={policy.vehicle?.chassisNumber ?? "—"} />
                <Row label="Engine" value={policy.vehicle?.engineNumber ?? "—"} />
              </CardContent>
            </Card>
          </div>
          <ConfirmDialog
            open={cancelConfirm.open}
            onOpenChange={(v) => !v && cancelConfirm.close()}
            title="Cancel this policy?"
            description="The policy will be marked CANCELLED. This cannot be undone."
            confirmLabel="Cancel policy"
            onConfirm={() => policyService.cancel(cancelConfirm.target!)}
            onDone={() => {
              cancelConfirm.close();
              refetch();
            }}
          />
        </>
      )}
    </>
  );
}
