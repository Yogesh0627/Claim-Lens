"use client";

import { PageHeader } from "@/components/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DataState, EmptyState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { useAsync } from "@/hooks/useAsync";
import { portalService } from "@/services/portalService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { formatCurrency } from "@/lib/format";
import { formatDate } from "@/lib/dayjs";

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
              <Card key={p.id}>
                <CardHeader className="flex flex-row items-center justify-between gap-2">
                  <CardTitle className="text-base">{p.policyNumber}</CardTitle>
                  <StatusBadge value={p.status} map={GENERIC_STATUS_META} />
                </CardHeader>
                <CardContent className="text-sm">
                  <dl className="grid grid-cols-2 gap-y-2">
                    <dt className="text-muted-foreground">Vehicle</dt>
                    <dd className="text-right font-medium">
                      {p.vehicle?.registrationNumber ?? "—"}
                    </dd>
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
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </DataState>
    </>
  );
}
