"use client";

import { Pencil, Plus, Trash2 } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { IconButton } from "@/components/icon-button";
import { SimpleOrgDialog, useCrudDialog } from "@/components/organization/simple-org-dialog";
import { ConfirmDialog, useConfirm } from "@/components/confirm-dialog";
import { useAsync } from "@/hooks/useAsync";
import { organizationService } from "@/services/organizationService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { PERMISSIONS } from "@/lib/permissions";
import type { DesignationResponse } from "@/lib/types";

export default function DesignationsPage() {
  const { data, loading, error, refetch } = useAsync(
    () => organizationService.listDesignations(),
    [],
  );
  const dialog = useCrudDialog<DesignationResponse>();
  const confirm = useConfirm<DesignationResponse>();

  const columns: Column<DesignationResponse>[] = [
    { header: "Code", cell: (d) => <span className="font-medium">{d.code}</span> },
    { header: "Name", cell: (d) => d.name },
    { header: "Description", cell: (d) => d.description ?? "—" },
    { header: "Status", cell: (d) => <StatusBadge value={d.status} map={GENERIC_STATUS_META} /> },
    {
      header: "",
      headerClassName: "w-24",
      cell: (d) => (
        <Can permission={PERMISSIONS.ORG_DESIGNATION_WRITE}>
          <div className="flex justify-end">
            <IconButton label="Edit" onClick={() => dialog.openEdit(d)}>
              <Pencil className="h-4 w-4" />
            </IconButton>
            <IconButton label="Delete" onClick={() => confirm.ask(d)}>
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
        title="Designations"
        actions={
          <Can permission={PERMISSIONS.ORG_DESIGNATION_WRITE}>
            <Button onClick={dialog.openCreate}>
              <Plus className="mr-1 h-4 w-4" /> New designation
            </Button>
          </Can>
        }
      />
      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={<EmptyState title="No designations" />}
      >
        {(rows) => <DataTable columns={columns} rows={rows} getKey={(d) => d.id} />}
      </DataState>

      <SimpleOrgDialog
        title="designation"
        open={dialog.open}
        onOpenChange={dialog.setOpen}
        entity={dialog.editing}
        onCreate={(body) => organizationService.createDesignation(body)}
        onUpdate={(id, body) => organizationService.updateDesignation(id, body)}
        onSaved={refetch}
      />
      <ConfirmDialog
        open={confirm.open}
        onOpenChange={(v) => !v && confirm.close()}
        title="Delete designation?"
        description={confirm.target ? `This removes "${confirm.target.name}".` : undefined}
        onConfirm={() => organizationService.deleteDesignation(confirm.target!.id)}
        onDone={refetch}
      />
    </>
  );
}
