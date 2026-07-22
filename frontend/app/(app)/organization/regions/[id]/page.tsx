"use client";

import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Pencil, Plus, Trash2 } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { IconButton } from "@/components/icon-button";
import { BranchDialog } from "@/components/organization/branch-dialog";
import { useCrudDialog } from "@/components/organization/simple-org-dialog";
import { ConfirmDialog, useConfirm } from "@/components/confirm-dialog";
import { useAsync } from "@/hooks/useAsync";
import { organizationService } from "@/services/organizationService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { PERMISSIONS } from "@/lib/permissions";
import type { BranchResponse } from "@/lib/types";

export default function RegionBranchesPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const regionId = Number(params.id);

  const { data, loading, error, refetch } = useAsync(
    () => organizationService.listBranches(regionId),
    [regionId],
  );
  const dialog = useCrudDialog<BranchResponse>();
  const confirm = useConfirm<BranchResponse>();

  const columns: Column<BranchResponse>[] = [
    { header: "Code", cell: (b) => <span className="font-medium">{b.code}</span> },
    { header: "Name", cell: (b) => b.name },
    { header: "City", cell: (b) => b.city ?? "—" },
    { header: "Email", cell: (b) => b.email ?? "—" },
    { header: "Status", cell: (b) => <StatusBadge value={b.status} map={GENERIC_STATUS_META} /> },
    {
      header: "",
      headerClassName: "w-24",
      cell: (b) => (
        <Can permission={PERMISSIONS.ORG_BRANCH_WRITE}>
          <div className="flex justify-end">
            <IconButton label="Edit" onClick={() => dialog.openEdit(b)}>
              <Pencil className="h-4 w-4" />
            </IconButton>
            <IconButton label="Delete" onClick={() => confirm.ask(b)}>
              <Trash2 className="text-destructive h-4 w-4" />
            </IconButton>
          </div>
        </Can>
      ),
    },
  ];

  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        className="-ml-2 w-fit"
        onClick={() => router.push("/organization/regions")}
      >
        <ArrowLeft className="mr-1 h-4 w-4" /> Regions
      </Button>
      <PageHeader
        title="Branches"
        description="Branches in this region"
        actions={
          <Can permission={PERMISSIONS.ORG_BRANCH_WRITE}>
            <Button onClick={dialog.openCreate}>
              <Plus className="mr-1 h-4 w-4" /> New branch
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
          <EmptyState title="No branches" description="Add the first branch to this region." />
        }
      >
        {(rows) => <DataTable columns={columns} rows={rows} getKey={(b) => b.id} />}
      </DataState>

      <BranchDialog
        regionId={regionId}
        open={dialog.open}
        onOpenChange={dialog.setOpen}
        branch={dialog.editing}
        onSaved={refetch}
      />
      <ConfirmDialog
        open={confirm.open}
        onOpenChange={(v) => !v && confirm.close()}
        title="Delete branch?"
        description={confirm.target ? `This removes "${confirm.target.name}".` : undefined}
        onConfirm={() => organizationService.deleteBranch(regionId, confirm.target!.id)}
        onDone={refetch}
      />
    </>
  );
}
