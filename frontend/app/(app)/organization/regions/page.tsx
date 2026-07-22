"use client";

import { useRouter } from "next/navigation";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { IconButton } from "@/components/icon-button";
import { RegionDialog } from "@/components/organization/region-dialog";
import { useCrudDialog } from "@/components/organization/simple-org-dialog";
import { ConfirmDialog, useConfirm } from "@/components/confirm-dialog";
import { useAsync } from "@/hooks/useAsync";
import { organizationService } from "@/services/organizationService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { PERMISSIONS } from "@/lib/permissions";
import type { RegionResponse } from "@/lib/types";

export default function RegionsPage() {
  const router = useRouter();
  const { data, loading, error, refetch } = useAsync(() => organizationService.listRegions(), []);
  const dialog = useCrudDialog<RegionResponse>();
  const confirm = useConfirm<RegionResponse>();

  const columns: Column<RegionResponse>[] = [
    { header: "Code", cell: (r) => <span className="font-medium">{r.code}</span> },
    { header: "Name", cell: (r) => r.name },
    { header: "Status", cell: (r) => <StatusBadge value={r.status} map={GENERIC_STATUS_META} /> },
    {
      header: "",
      headerClassName: "w-24",
      cell: (r) => (
        <Can permission={PERMISSIONS.ORG_REGION_WRITE}>
          <div className="flex justify-end" onClick={(e) => e.stopPropagation()}>
            <IconButton label="Edit" onClick={() => dialog.openEdit(r)}>
              <Pencil className="h-4 w-4" />
            </IconButton>
            <IconButton label="Delete" onClick={() => confirm.ask(r)}>
              <Trash2 className="text-destructive h-4 w-4" />
            </IconButton>
          </div>
        </Can>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Regions & branches"
        description="Open a region to manage its branches"
        actions={
          <Can permission={PERMISSIONS.ORG_REGION_WRITE}>
            <Button onClick={dialog.openCreate}>
              <Plus className="mr-1 h-4 w-4" /> New region
            </Button>
          </Can>
        }
      />
      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={<EmptyState title="No regions" />}
      >
        {(rows) => (
          <DataTable
            columns={columns}
            rows={rows}
            getKey={(r) => r.id}
            onRowClick={(r) => router.push(`/organization/regions/${r.id}`)}
          />
        )}
      </DataState>

      <RegionDialog
        open={dialog.open}
        onOpenChange={dialog.setOpen}
        region={dialog.editing}
        onSaved={refetch}
      />
      <ConfirmDialog
        open={confirm.open}
        onOpenChange={(v) => !v && confirm.close()}
        title="Delete region?"
        description={confirm.target ? `This removes "${confirm.target.name}".` : undefined}
        onConfirm={() => organizationService.deleteRegion(confirm.target!.id)}
        onDone={refetch}
      />
    </>
  );
}
