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
import { ProductFormDialog } from "@/components/products/product-dialogs";
import { useAsync } from "@/hooks/useAsync";
import { productService } from "@/services/productService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { formatDate } from "@/lib/dayjs";
import { PERMISSIONS } from "@/lib/permissions";
import type { ProductResponse } from "@/lib/types";

export default function ProductsPage() {
  const router = useRouter();
  const { data, loading, error, refetch } = useAsync(() => productService.list(), []);
  const [open, setOpen] = useState(false);

  const columns: Column<ProductResponse>[] = [
    { header: "Code", cell: (p) => <span className="font-medium">{p.code}</span> },
    { header: "Name", cell: (p) => p.name },
    { header: "Created", cell: (p) => formatDate(p.createdAt) },
    { header: "Status", cell: (p) => <StatusBadge value={p.status} map={GENERIC_STATUS_META} /> },
  ];

  return (
    <>
      <PageHeader
        title="Products"
        description="Insurance products and their versions"
        actions={
          <Can permission={PERMISSIONS.PRODUCT_WRITE}>
            <Button onClick={() => setOpen(true)}>
              <Plus className="mr-1 h-4 w-4" /> New product
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
            title="No products"
            action={
              <Can permission={PERMISSIONS.PRODUCT_WRITE}>
                <Button onClick={() => setOpen(true)}>
                  <Plus className="mr-1 h-4 w-4" /> New product
                </Button>
              </Can>
            }
          />
        }
      >
        {(products) => (
          <DataTable
            columns={columns}
            rows={products}
            getKey={(p) => p.id}
            onRowClick={(p) => router.push(`/products/${p.id}`)}
          />
        )}
      </DataState>

      <ProductFormDialog open={open} onOpenChange={setOpen} onSaved={refetch} />
    </>
  );
}
