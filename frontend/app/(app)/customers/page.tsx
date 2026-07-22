"use client";

import { Pencil, Plus } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { Button } from "@/components/ui/button";
import { IconButton } from "@/components/icon-button";
import { CustomerFormDialog, useCustomerDialog } from "@/components/customers/customer-form-dialog";
import { useAsync } from "@/hooks/useAsync";
import { customerService } from "@/services/customerService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { formatDate } from "@/lib/dayjs";
import { PERMISSIONS } from "@/lib/permissions";
import type { CustomerResponse } from "@/lib/types";

export default function CustomersPage() {
  const { data, loading, error, refetch } = useAsync(() => customerService.list(), []);
  const dialog = useCustomerDialog();

  const columns: Column<CustomerResponse>[] = [
    { header: "Number", cell: (c) => <span className="font-medium">{c.customerNumber}</span> },
    { header: "Name", cell: (c) => `${c.firstName} ${c.lastName ?? ""}`.trim() },
    { header: "Email", cell: (c) => c.email ?? "—" },
    { header: "Phone", cell: (c) => c.phone ?? "—" },
    { header: "DOB", cell: (c) => formatDate(c.dateOfBirth) },
    { header: "Status", cell: (c) => <StatusBadge value={c.status} map={GENERIC_STATUS_META} /> },
    {
      header: "",
      headerClassName: "w-10",
      cell: (c) => (
        <Can permission={PERMISSIONS.CUSTOMER_WRITE}>
          <IconButton
            label="Edit customer"
            onClick={(e) => {
              e.stopPropagation();
              dialog.openEdit(c);
            }}
          >
            <Pencil className="h-4 w-4" />
          </IconButton>
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
        data={data}
        emptyWhen={(d) => d.length === 0}
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
        {(customers) => <DataTable columns={columns} rows={customers} getKey={(c) => c.id} />}
      </DataState>

      <CustomerFormDialog
        open={dialog.open}
        onOpenChange={dialog.setOpen}
        customer={dialog.editing}
        onSaved={refetch}
      />
    </>
  );
}
