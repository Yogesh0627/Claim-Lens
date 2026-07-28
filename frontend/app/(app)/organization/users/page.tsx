"use client";

import { useState } from "react";
import { Pencil, Plus } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { PaginationBar } from "@/components/pagination-bar";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { IconButton } from "@/components/icon-button";
import { UserFormDialog } from "@/components/users/user-form-dialog";
import { usePaginated } from "@/hooks/usePaginated";
import { userService } from "@/services/userService";
import { GENERIC_STATUS_META, optionLabel } from "@/lib/enums";
import { PERMISSIONS } from "@/lib/permissions";
import type { UserResponse } from "@/lib/types";

export default function UsersPage() {
  const { meta, setPage, loading, error, refetch } = usePaginated(userService.list);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<UserResponse | null>(null);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
  };
  const openEdit = (u: UserResponse) => {
    setEditing(u);
    setOpen(true);
  };

  const columns: Column<UserResponse>[] = [
    {
      header: "Name",
      cell: (u) => <span className="font-medium">{`${u.firstName} ${u.lastName ?? ""}`.trim()}</span>,
    },
    { header: "Email", cell: (u) => u.email },
    { header: "Employee", cell: (u) => u.employeeCode },
    { header: "Role", cell: (u) => (u.roleCode ? optionLabel(u.roleCode) : "—") },
    {
      header: "Department",
      cell: (u) => u.departmentName ?? "—",
    },
    {
      header: "Branch",
      cell: (u) => u.homeBranchName ?? "—",
    },
    { header: "Status", cell: (u) => <StatusBadge value={u.status} map={GENERIC_STATUS_META} /> },
    {
      header: "",
      headerClassName: "w-10",
      cell: (u) => (
        <Can permission={PERMISSIONS.USER_WRITE}>
          <div className="flex justify-end">
            <IconButton label="Edit user" onClick={() => openEdit(u)}>
              <Pencil className="h-4 w-4" />
            </IconButton>
          </div>
        </Can>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Users"
        description="Manage the people in your tenant and their roles"
        actions={
          <Can permission={PERMISSIONS.USER_WRITE}>
            <Button onClick={openCreate}>
              <Plus className="mr-1 h-4 w-4" /> New user
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
            title="No users"
            action={
              <Can permission={PERMISSIONS.USER_WRITE}>
                <Button onClick={openCreate}>
                  <Plus className="mr-1 h-4 w-4" /> New user
                </Button>
              </Can>
            }
          />
        }
      >
        {(m) => (
          <div className="space-y-4">
            <DataTable columns={columns} rows={m.content} getKey={(u) => u.id} />
            <PaginationBar meta={m} onPageChange={setPage} label="users" />
          </div>
        )}
      </DataState>

      <UserFormDialog open={open} onOpenChange={setOpen} user={editing} onSaved={refetch} />
    </>
  );
}
