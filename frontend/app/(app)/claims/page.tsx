"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { useAsync } from "@/hooks/useAsync";
import { claimService } from "@/services/claimService";
import { CLAIM_STATUS_META } from "@/lib/enums";
import { formatCurrency } from "@/lib/format";
import { formatDate } from "@/lib/dayjs";
import { PERMISSIONS } from "@/lib/permissions";
import type { ClaimResponse } from "@/lib/types";

export default function ClaimsPage() {
  const router = useRouter();
  const { data, loading, error } = useAsync(() => claimService.list(), []);

  const columns: Column<ClaimResponse>[] = [
    { header: "Claim #", cell: (c) => <span className="font-medium">{c.claimNumber}</span> },
    { header: "Policy", cell: (c) => c.policyNumber ?? "—" },
    { header: "Vehicle", cell: (c) => c.vehicleRegistrationNumber ?? "—" },
    { header: "Incident", cell: (c) => formatDate(c.incidentDate) },
    {
      header: "Amount",
      cell: (c) => formatCurrency(c.claimAmount),
      className: "text-right tabular-nums",
      headerClassName: "text-right",
    },
    { header: "Status", cell: (c) => <StatusBadge value={c.status} map={CLAIM_STATUS_META} /> },
  ];

  return (
    <>
      <PageHeader
        title="Claims"
        description="Motor insurance claims across the portfolio"
        actions={
          <Can permission={PERMISSIONS.CLAIM_WRITE}>
            <Button asChild>
              <Link href="/claims/new">
                <Plus className="mr-1 h-4 w-4" /> New claim
              </Link>
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
            title="No claims yet"
            description="Create a draft claim to get started."
            action={
              <Can permission={PERMISSIONS.CLAIM_WRITE}>
                <Button asChild>
                  <Link href="/claims/new">
                    <Plus className="mr-1 h-4 w-4" /> New claim
                  </Link>
                </Button>
              </Can>
            }
          />
        }
      >
        {(claims) => (
          <DataTable
            columns={columns}
            rows={claims}
            getKey={(c) => c.id}
            onRowClick={(c) => router.push(`/claims/${c.id}`)}
          />
        )}
      </DataState>
    </>
  );
}
