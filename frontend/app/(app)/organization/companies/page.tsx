"use client";

import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { useAsync } from "@/hooks/useAsync";
import { organizationService } from "@/services/organizationService";
import { GENERIC_STATUS_META, optionLabel } from "@/lib/enums";
import { formatDate } from "@/lib/dayjs";
import type { InsuranceCompanyResponse } from "@/lib/types";

export default function CompaniesPage() {
  const { data, loading, error } = useAsync(() => organizationService.listCompanies(), []);

  const columns: Column<InsuranceCompanyResponse>[] = [
    { header: "Name", cell: (c) => <span className="font-medium">{c.name}</span> },
    { header: "Code", cell: (c) => c.code },
    { header: "Plan", cell: (c) => optionLabel(c.subscriptionPlan) },
    { header: "Currency", cell: (c) => c.currency },
    { header: "Timezone", cell: (c) => c.timezone },
    { header: "Created", cell: (c) => formatDate(c.createdAt) },
    { header: "Status", cell: (c) => <StatusBadge value={c.status} map={GENERIC_STATUS_META} /> },
  ];

  return (
    <>
      <PageHeader title="Companies" description="Insurance company (tenant) profile" />
      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={<EmptyState title="No companies" />}
      >
        {(rows) => <DataTable columns={columns} rows={rows} getKey={(c) => c.id} />}
      </DataState>
    </>
  );
}
