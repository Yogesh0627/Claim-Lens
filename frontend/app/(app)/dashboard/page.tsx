"use client";

import { FileText, ShieldAlert } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { StatusBadge } from "@/components/status-badge";
import { DataState } from "@/components/data-state";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAsync } from "@/hooks/useAsync";
import { analyticsService } from "@/services/miscService";
import { CLAIM_STATUS_META, RISK_META, statusMeta } from "@/lib/enums";
import { formatNumber } from "@/lib/format";
import type { DashboardResponse } from "@/lib/types";

const RISK_BAR: Record<string, string> = {
  LOW: "bg-emerald-500",
  MEDIUM: "bg-amber-500",
  HIGH: "bg-red-500",
};

function Summary({ data }: { data: DashboardResponse }) {
  const statusEntries = Object.entries(data.claimsByStatus).sort((a, b) => b[1] - a[1]);
  const totalClaims = statusEntries.reduce((sum, [, n]) => sum + n, 0);
  const riskEntries = ["HIGH", "MEDIUM", "LOW"].map(
    (k) => [k, data.fraudRiskDistribution[k] ?? 0] as const,
  );
  const totalScored = riskEntries.reduce((sum, [, n]) => sum + n, 0);

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-muted-foreground flex items-center gap-2 text-sm font-medium">
            <FileText className="h-4 w-4" /> Total claims
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">{formatNumber(totalClaims)}</p>
        </CardContent>
      </Card>

      <Card className="lg:col-span-2">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium">Claims by status</CardTitle>
        </CardHeader>
        <CardContent>
          {statusEntries.length === 0 ? (
            <p className="text-muted-foreground text-sm">No claims yet.</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {statusEntries.map(([status, count]) => (
                <div
                  key={status}
                  className="flex items-center gap-2 rounded-md border px-2.5 py-1.5"
                >
                  <StatusBadge value={status} map={CLAIM_STATUS_META} />
                  <span className="text-sm font-semibold">{formatNumber(count)}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="lg:col-span-3">
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 text-sm font-medium">
            <ShieldAlert className="h-4 w-4" /> Fraud risk distribution
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {totalScored === 0 ? (
            <p className="text-muted-foreground text-sm">No scored claims yet.</p>
          ) : (
            riskEntries.map(([risk, count]) => {
              const pct = totalScored ? Math.round((count / totalScored) * 100) : 0;
              return (
                <div key={risk} className="space-y-1">
                  <div className="flex items-center justify-between text-sm">
                    <span>{statusMeta(RISK_META, risk).label} risk</span>
                    <span className="text-muted-foreground">
                      {formatNumber(count)} ({pct}%)
                    </span>
                  </div>
                  <div className="bg-muted h-2 w-full overflow-hidden rounded-full">
                    <div
                      className={`h-full ${RISK_BAR[risk] ?? "bg-primary"}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default function DashboardPage() {
  const { data, loading, error } = useAsync(() => analyticsService.dashboard(), []);
  return (
    <>
      <PageHeader title="Dashboard" description="Portfolio overview of claims and fraud risk" />
      <DataState loading={loading} error={error} data={data}>
        {(d) => <Summary data={d} />}
      </DataState>
    </>
  );
}
