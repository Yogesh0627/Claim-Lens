"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { RulesetFormDialog } from "@/components/rulesets/ruleset-form-dialog";
import { useAsync } from "@/hooks/useAsync";
import { rulesetService } from "@/services/rulesetService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { PERMISSIONS } from "@/lib/permissions";
import type { FraudRulesetResponse } from "@/lib/types";

export default function RulesetsPage() {
  const router = useRouter();
  const { data, loading, error, refetch } = useAsync(() => rulesetService.list(), []);
  const [open, setOpen] = useState(false);

  const columns: Column<FraudRulesetResponse>[] = [
    { header: "Name", cell: (r) => <span className="font-medium">{r.name}</span> },
    { header: "Rules", cell: (r) => r.rules.length },
    { header: "Medium ≥", cell: (r) => r.mediumThreshold },
    { header: "High ≥", cell: (r) => r.highThreshold },
    { header: "Status", cell: (r) => <StatusBadge value={r.status} map={GENERIC_STATUS_META} /> },
  ];

  return (
    <>
      <PageHeader
        title="Fraud rulesets"
        description="Weighted rules and score thresholds per claim type"
        actions={
          <Can permission={PERMISSIONS.RULESET_WRITE}>
            <Button onClick={() => setOpen(true)}>
              <Plus className="mr-1 h-4 w-4" /> New ruleset
            </Button>
          </Can>
        }
      />
      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={
          <EmptyState
            title="No rulesets"
            description="The engine falls back to defaults until a ruleset is active."
            action={
              <Can permission={PERMISSIONS.RULESET_WRITE}>
                <Button onClick={() => setOpen(true)}>
                  <Plus className="mr-1 h-4 w-4" /> New ruleset
                </Button>
              </Can>
            }
          />
        }
      >
        {(rulesets) => (
          <DataTable
            columns={columns}
            rows={rulesets}
            getKey={(r) => r.id}
            onRowClick={(r) => router.push(`/rulesets/${r.id}`)}
          />
        )}
      </DataState>

      <RulesetFormDialog open={open} onOpenChange={setOpen} onSaved={refetch} />
    </>
  );
}
