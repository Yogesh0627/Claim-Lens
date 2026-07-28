"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { PaginationBar } from "@/components/pagination-bar";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { PolicyFormDialog } from "@/components/policies/policy-form-dialog";
import { usePaginated } from "@/hooks/usePaginated";
import { policyService } from "@/services/policyService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { formatCurrency } from "@/lib/format";
import { formatDate } from "@/lib/dayjs";
import { PERMISSIONS } from "@/lib/permissions";
import type { PolicyResponse } from "@/lib/types";

export default function PoliciesPage() {
  const router = useRouter();
  const { meta, setPage, loading, error, refetch } = usePaginated(policyService.list);
  const [open, setOpen] = useState(false);

  const columns: Column<PolicyResponse>[] = [
    { header: "Policy #", cell: (p) => <span className="font-medium">{p.policyNumber}</span> },
    { header: "Vehicle", cell: (p) => p.vehicle?.registrationNumber ?? "—" },
    {
      header: "Effective",
      cell: (p) => `${formatDate(p.effectiveFrom)} → ${formatDate(p.effectiveTo)}`,
    },
    {
      header: "Sum insured",
      cell: (p) => formatCurrency(p.sumInsured, p.currency ?? "INR"),
      className: "text-right tabular-nums",
      headerClassName: "text-right",
    },
    { header: "Status", cell: (p) => <StatusBadge value={p.status} map={GENERIC_STATUS_META} /> },
  ];

  return (
    <>
      <PageHeader
        title="Policies"
        description="Insurance contracts and their insured vehicles"
        actions={
          <Can permission={PERMISSIONS.POLICY_WRITE}>
            <Button onClick={() => setOpen(true)}>
              <Plus className="mr-1 h-4 w-4" /> New policy
            </Button>
          </Can>
        }
      />
      <DataState
        loading={loading}
        error={error}
        data={meta}
        emptyWhen={(m) => m.totalElements === 0}
        empty={
          <EmptyState
            title="No policies"
            action={
              <Can permission={PERMISSIONS.POLICY_WRITE}>
                <Button onClick={() => setOpen(true)}>
                  <Plus className="mr-1 h-4 w-4" /> New policy
                </Button>
              </Can>
            }
          />
        }
      >
        {(m) => (
          <div className="space-y-4">
            <DataTable
              columns={columns}
              rows={m.content}
              getKey={(p) => p.id}
              onRowClick={(p) => router.push(`/policies/${p.id}`)}
            />
            <PaginationBar meta={m} onPageChange={setPage} label="policies" />
          </div>
        )}
      </DataState>

      <PolicyFormDialog open={open} onOpenChange={setOpen} onSaved={refetch} />
    </>
  );
}
