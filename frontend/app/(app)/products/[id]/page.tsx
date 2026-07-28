"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, BookOpen, FileText, Plus, CheckCircle2, Pencil } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { LoadingRows, ErrorState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { VersionFormDialog, EditProductDialog } from "@/components/products/product-dialogs";
import { KnowledgeDialog } from "@/components/products/knowledge-dialog";
import { WordingDialog } from "@/components/products/wording-dialog";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { productService } from "@/services/productService";
import { GENERIC_STATUS_META } from "@/lib/enums";
import { formatDate } from "@/lib/dayjs";
import { PERMISSIONS } from "@/lib/permissions";
import type { ProductVersionResponse } from "@/lib/types";

export default function ProductDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const productId = Number(params.id);
  const [open, setOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [knowledgeVersion, setKnowledgeVersion] = useState<ProductVersionResponse | null>(null);
  const [wordingVersion, setWordingVersion] = useState<number | null>(null);

  const product = useAsync(() => productService.get(productId), [productId]);
  const versions = useAsync(() => productService.listVersions(productId), [productId]);

  const activate = useMutation(
    (versionId: number) => productService.activateVersion(productId, versionId),
    { successMessage: "Version activated", onSuccess: () => versions.refetch() },
  );

  const columns: Column<ProductVersionResponse>[] = [
    { header: "Version", cell: (v) => <span className="font-medium">v{v.versionNumber}</span> },
    { header: "Effective from", cell: (v) => formatDate(v.effectiveFrom) },
    { header: "Effective to", cell: (v) => formatDate(v.effectiveTo) },
    { header: "Status", cell: (v) => <StatusBadge value={v.status} map={GENERIC_STATUS_META} /> },
    {
      header: "",
      headerClassName: "w-56",
      cell: (v) => (
        <div className="flex justify-end gap-2">
          <Can permission={PERMISSIONS.COVERAGE_WRITE}>
            <Button variant="outline" size="sm" onClick={() => setKnowledgeVersion(v)}>
              <BookOpen className="mr-1 h-4 w-4" /> Knowledge
            </Button>
          </Can>
          <Can permission={PERMISSIONS.PRODUCT_WRITE}>
            <Button variant="outline" size="sm" onClick={() => setWordingVersion(v.id)}>
              <FileText className="mr-1 h-4 w-4" /> Wording
            </Button>
          </Can>
          {v.status !== "ACTIVE" ? (
            <Can permission={PERMISSIONS.PRODUCT_WRITE}>
              <Button
                variant="outline"
                size="sm"
                onClick={() => activate.run(v.id)}
                disabled={activate.loading}
              >
                <CheckCircle2 className="mr-1 h-4 w-4" /> Activate
              </Button>
            </Can>
          ) : null}
        </div>
      ),
    },
  ];

  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        className="-ml-2 w-fit"
        onClick={() => router.push("/products")}
      >
        <ArrowLeft className="mr-1 h-4 w-4" /> Products
      </Button>

      {product.loading ? (
        <LoadingRows />
      ) : product.error || !product.data ? (
        <ErrorState message={product.error ?? "Product not found"} />
      ) : (
        <>
          <PageHeader
            title={product.data.name}
            description={`Code ${product.data.code}`}
            actions={
              <Can permission={PERMISSIONS.PRODUCT_WRITE}>
                <div className="flex gap-2">
                  <Button variant="outline" onClick={() => setEditOpen(true)}>
                    <Pencil className="mr-1 h-4 w-4" /> Edit
                  </Button>
                  <Button onClick={() => setOpen(true)}>
                    <Plus className="mr-1 h-4 w-4" /> New version
                  </Button>
                </div>
              </Can>
            }
          />

          {versions.loading ? (
            <LoadingRows />
          ) : versions.error ? (
            <ErrorState message={versions.error} />
          ) : (versions.data ?? []).length === 0 ? (
            <EmptyState title="No versions" description="Create the first product version." />
          ) : (
            <DataTable columns={columns} rows={versions.data ?? []} getKey={(v) => v.id} />
          )}

          <VersionFormDialog
            productId={productId}
            open={open}
            onOpenChange={setOpen}
            onSaved={versions.refetch}
          />
          <EditProductDialog
            product={product.data}
            open={editOpen}
            onOpenChange={setEditOpen}
            onSaved={product.refetch}
          />
          {knowledgeVersion != null ? (
            <KnowledgeDialog
              productId={productId}
              versionId={knowledgeVersion.id}
              versionNumber={knowledgeVersion.versionNumber}
              open={knowledgeVersion != null}
              onOpenChange={(v) => !v && setKnowledgeVersion(null)}
            />
          ) : null}
          {wordingVersion != null ? (
            <WordingDialog
              productId={productId}
              versionId={wordingVersion}
              open={wordingVersion != null}
              onOpenChange={(v) => !v && setWordingVersion(null)}
            />
          ) : null}
        </>
      )}
    </>
  );
}
