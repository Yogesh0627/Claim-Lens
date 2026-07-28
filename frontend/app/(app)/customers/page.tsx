"use client";

import { Pencil, Plus, Trash2 } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { PaginationBar } from "@/components/pagination-bar";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { IconButton } from "@/components/icon-button";
import { CustomerFormDialog, useCustomerDialog } from "@/components/customers/customer-form-dialog";
import { ConfirmDialog, useConfirm } from "@/components/confirm-dialog";
import { usePaginated } from "@/hooks/usePaginated";
import { customerService } from "@/services/customerService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { formatDate } from "@/lib/dayjs";
import { PERMISSIONS } from "@/lib/permissions";
import type { CustomerResponse } from "@/lib/types";

export default function CustomersPage() {
  const { meta, setPage, loading, error, refetch } = usePaginated(customerService.list);
  const dialog = useCustomerDialog();
  const confirm = useConfirm<CustomerResponse>();

  const columns: Column<CustomerResponse>[] = [
    { header: "Number", cell: (c) => <span className="font-medium">{c.customerNumber}</span> },
    { header: "Name", cell: (c) => `${c.firstName} ${c.lastName ?? ""}`.trim() },
    { header: "Email", cell: (c) => c.email ?? "—" },
    { header: "Phone", cell: (c) => c.phone ?? "—" },
    { header: "DOB", cell: (c) => formatDate(c.dateOfBirth) },
    { header: "Status", cell: (c) => <StatusBadge value={c.status} map={GENERIC_STATUS_META} /> },
    {
      header: "",
      headerClassName: "w-24",
      cell: (c) => (
        <Can permission={PERMISSIONS.CUSTOMER_WRITE}>
          <div className="flex justify-end">
            <IconButton
              label="Edit customer"
              onClick={(e) => {
                e.stopPropagation();
                dialog.openEdit(c);
              }}
            >
              <Pencil className="h-4 w-4" />
            </IconButton>
            <IconButton
              label="Delete customer"
              onClick={(e) => {
                e.stopPropagation();
                confirm.ask(c);
              }}
            >
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
        title="Customers"
        description="Policyholders in your tenant"
        actions={
          <Can permission={PERMISSIONS.CUSTOMER_WRITE}>
            <Button onClick={dialog.openCreate}>
              <Plus className="mr-1 h-4 w-4" /> New customer
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
            title="No customers"
            action={
              <Can permission={PERMISSIONS.CUSTOMER_WRITE}>
                <Button onClick={dialog.openCreate}>
                  <Plus className="mr-1 h-4 w-4" /> New customer
                </Button>
              </Can>
            }
          />
        }
      >
        {(m) => (
          <div className="space-y-4">
            <DataTable columns={columns} rows={m.content} getKey={(c) => c.id} />
            <PaginationBar meta={m} onPageChange={setPage} label="customers" />
          </div>
        )}
      </DataState>

      <CustomerFormDialog
        open={dialog.open}
        onOpenChange={dialog.setOpen}
        customer={dialog.editing}
        onSaved={refetch}
      />
      <ConfirmDialog
        open={confirm.open}
        onOpenChange={(v) => !v && confirm.close()}
        title="Delete customer?"
        description={
          confirm.target
            ? `This removes ${confirm.target.firstName} ${confirm.target.lastName ?? ""}`.trim() + "."
            : undefined
        }
        onConfirm={() => customerService.remove(confirm.target!.id)}
        onDone={refetch}
      />
    </>
  );
}
