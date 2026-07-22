"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Ban, CheckCircle2, LogIn, Plus } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { OnboardTenantDialog } from "@/components/platform/onboard-tenant-dialog";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { useAppDispatch } from "@/hooks/redux";
import { enterTenant } from "@/store/authSlice";
import { platformService } from "@/services/platformService";
import { GENERIC_STATUS_META, optionLabel } from "@/lib/enums";
import { formatDate } from "@/lib/dayjs";
import { homePathFor } from "@/lib/navigation";
import { isApiError } from "@/lib/http";
import { toast } from "sonner";
import type { InsuranceCompanyResponse } from "@/lib/types";

export default function PlatformTenantsPage() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { data, loading, error, refetch } = useAsync(() => platformService.tenants(), []);
  const [onboardOpen, setOnboardOpen] = useState(false);

  const setStatus = useMutation(
    (args: { id: number; status: string }) => platformService.setStatus(args.id, args.status),
    { successMessage: "Tenant updated", onSuccess: () => refetch() },
  );

  const [entering, setEntering] = useState<number | null>(null);
  const openWorkspace = async (t: InsuranceCompanyResponse) => {
    setEntering(t.id);
    try {
      const res = await dispatch(enterTenant({ id: t.id, name: t.name })).unwrap();
      router.push(homePathFor(new Set(res.me.permissions)));
    } catch (e) {
      toast.error(isApiError(e) ? e.message : "Could not open tenant workspace");
      setEntering(null);
    }
  };

  const columns: Column<InsuranceCompanyResponse>[] = [
    { header: "Tenant", cell: (t) => <span className="font-medium">{t.name}</span> },
    { header: "Code", cell: (t) => t.code },
    { header: "Plan", cell: (t) => optionLabel(String(t.subscriptionPlan)) },
    { header: "Created", cell: (t) => formatDate(t.createdAt) },
    {
      header: "Status",
      cell: (t) => <StatusBadge value={String(t.status)} map={GENERIC_STATUS_META} />,
    },
    {
      header: "",
      headerClassName: "w-72",
      cell: (t) => (
        <div className="flex justify-end gap-2">
          {String(t.status) === "ACTIVE" ? (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setStatus.run({ id: t.id, status: "SUSPENDED" })}
              disabled={setStatus.loading}
            >
              <Ban className="mr-1 h-4 w-4" /> Suspend
            </Button>
          ) : (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setStatus.run({ id: t.id, status: "ACTIVE" })}
              disabled={setStatus.loading}
            >
              <CheckCircle2 className="mr-1 h-4 w-4" /> Activate
            </Button>
          )}
          <Button size="sm" onClick={() => openWorkspace(t)} disabled={entering === t.id}>
            <LogIn className="mr-1 h-4 w-4" />
            {entering === t.id ? "Opening…" : "Open workspace"}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Tenants"
        description="Every insurance company on the platform. Open a workspace to act as that tenant."
        actions={
          <Button onClick={() => setOnboardOpen(true)}>
            <Plus className="mr-1 h-4 w-4" /> Onboard tenant
          </Button>
        }
      />
      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={
          <EmptyState
            title="No tenants yet"
            action={
              <Button onClick={() => setOnboardOpen(true)}>
                <Plus className="mr-1 h-4 w-4" /> Onboard tenant
              </Button>
            }
          />
        }
      >
        {(tenants) => <DataTable columns={columns} rows={tenants} getKey={(t) => t.id} />}
      </DataState>

      <OnboardTenantDialog open={onboardOpen} onOpenChange={setOnboardOpen} onSaved={refetch} />
    </>
  );
}
