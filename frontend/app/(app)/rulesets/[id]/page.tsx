"use client";

import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, CheckCircle2 } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { LoadingRows, ErrorState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { rulesetService } from "@/services/rulesetService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { PERMISSIONS } from "@/lib/permissions";
import type { FraudRuleResponse } from "@/lib/types";

export default function RulesetDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const rulesetId = Number(params.id);
  const { data, loading, error, refetch } = useAsync(
    () => rulesetService.get(rulesetId),
    [rulesetId],
  );

  const activate = useMutation(() => rulesetService.activate(rulesetId), {
    successMessage: "Ruleset activated",
    onSuccess: () => refetch(),
  });

  const columns: Column<FraudRuleResponse>[] = [
    { header: "Code", cell: (r) => <span className="font-medium">{r.code}</span> },
    { header: "Description", cell: (r) => r.description ?? "—" },
    {
      header: "Weight",
      cell: (r) => r.weight,
      className: "text-right",
      headerClassName: "text-right",
    },
    {
      header: "Enabled",
      cell: (r) => (
        <StatusBadge value={r.enabled ? "ACTIVE" : "INACTIVE"} map={GENERIC_STATUS_META} />
      ),
    },
  ];

  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        className="-ml-2 w-fit"
        onClick={() => router.push("/rulesets")}
      >
        <ArrowLeft className="mr-1 h-4 w-4" /> Rulesets
      </Button>

      {loading ? (
        <LoadingRows />
      ) : error || !data ? (
        <ErrorState message={error ?? "Ruleset not found"} />
      ) : (
        <>
          <PageHeader
            title={data.name}
            actions={
              data.status !== "ACTIVE" ? (
                <Can permission={PERMISSIONS.RULESET_WRITE}>
                  <Button onClick={() => activate.run()} disabled={activate.loading}>
                    <CheckCircle2 className="mr-1 h-4 w-4" /> Activate
                  </Button>
                </Can>
              ) : undefined
            }
          />
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge value={data.status} map={GENERIC_STATUS_META} />
            <Card className="px-3 py-1.5">
              <CardContent className="p-0 text-sm">
                Medium ≥ <b>{data.mediumThreshold}</b> · High ≥ <b>{data.highThreshold}</b>
              </CardContent>
            </Card>
          </div>
          <DataTable columns={columns} rows={data.rules} getKey={(r) => r.id} />
        </>
      )}
    </>
  );
}
