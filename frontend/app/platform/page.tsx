"use client";

import { Building2, FileText, ScrollText, Users } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MiniAreaChart, RiskBar, Sparkline } from "@/components/platform/analytics-charts";
import { useAsync } from "@/hooks/useAsync";
import { platformService } from "@/services/platformService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { formatNumber } from "@/lib/format";
import type { PlatformAnalyticsResponse, TenantStat } from "@/lib/types";

function Stat({
  icon: Icon,
  label,
  value,
  sub,
  trend,
}: {
  icon: typeof Building2;
  label: string;
  value: number;
  sub?: string;
  trend?: number[];
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-muted-foreground flex items-center gap-2 text-sm font-medium">
          <Icon className="h-4 w-4" /> {label}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-end justify-between gap-2">
          <div>
            <p className="text-3xl font-semibold tabular-nums">{formatNumber(value)}</p>
            {sub ? <p className="text-muted-foreground mt-1 text-xs">{sub}</p> : null}
          </div>
          {trend && trend.length > 1 ? (
            <div className="shrink-0 text-right">
              <Sparkline values={trend} />
              <p className="text-muted-foreground mt-0.5 text-[10px]">new / mo</p>
            </div>
          ) : null}
        </div>
      </CardContent>
    </Card>
  );
}

function Growth({ data }: { data: PlatformAnalyticsResponse["growth"] }) {
  const tenants = data.map((g) => ({ label: g.month, value: g.newTenants }));
  const claims = data.map((g) => ({ label: g.month, value: g.newClaims }));
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium">New tenants / month</CardTitle>
        </CardHeader>
        <CardContent>
          <MiniAreaChart points={tenants} unit="tenants" />
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium">New claims / month</CardTitle>
        </CardHeader>
        <CardContent>
          <MiniAreaChart points={claims} unit="claims" />
        </CardContent>
      </Card>
    </div>
  );
}

export default function PlatformOverviewPage() {
  const { data, loading, error } = useAsync(() => platformService.analytics(), []);

  const columns: Column<TenantStat>[] = [
    { header: "Tenant", cell: (t) => <span className="font-medium">{t.name}</span> },
    { header: "Status", cell: (t) => <StatusBadge value={t.status} map={GENERIC_STATUS_META} /> },
    { header: "Plan", cell: (t) => t.subscriptionPlan },
    { header: "Users", cell: (t) => formatNumber(t.users), className: "text-right tabular-nums" },
    { header: "Claims", cell: (t) => formatNumber(t.claims), className: "text-right tabular-nums" },
    {
      header: "Fraud risk mix",
      cell: (t) => (
        <div className="flex items-center gap-3">
          <RiskBar high={t.highRisk} medium={t.mediumRisk} low={t.lowRisk} />
          <span className="text-xs tabular-nums">
            <span className="text-red-600 dark:text-red-400">{t.highRisk}</span>
            {" · "}
            <span className="text-amber-600 dark:text-amber-400">{t.mediumRisk}</span>
            {" · "}
            <span className="text-emerald-600 dark:text-emerald-400">{t.lowRisk}</span>
          </span>
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Platform overview"
        description="Cross-tenant analytics across every insurance company on ClaimLens"
      />
      <DataState loading={loading} error={error} data={data}>
        {(d) => {
          const tenantTrend = d.growth.map((g) => g.newTenants);
          const claimTrend = d.growth.map((g) => g.newClaims);
          return (
            <div className="space-y-6">
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <Stat
                  icon={Building2}
                  label="Tenants"
                  value={d.totals.tenants}
                  sub={`${d.totals.activeTenants} active · ${d.totals.onboardingTenants} onboarding · ${d.totals.suspendedTenants} suspended`}
                  trend={tenantTrend}
                />
                <Stat icon={Users} label="Users" value={d.totals.users} />
                <Stat icon={FileText} label="Claims" value={d.totals.claims} trend={claimTrend} />
                <Stat icon={ScrollText} label="Policies" value={d.totals.policies} />
              </div>

              <Growth data={d.growth} />

              <div>
                <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <h2 className="text-sm font-semibold">Claims &amp; fraud by tenant</h2>
                  <div className="text-muted-foreground flex items-center gap-3 text-xs">
                    <span className="flex items-center gap-1">
                      <span className="h-2 w-2 rounded-full bg-red-500" /> High
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="h-2 w-2 rounded-full bg-amber-500" /> Medium
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="h-2 w-2 rounded-full bg-emerald-500" /> Low
                    </span>
                  </div>
                </div>
                <DataTable columns={columns} rows={d.tenants} getKey={(t) => t.tenantId} />
              </div>
            </div>
          );
        }}
      </DataState>
    </>
  );
}
