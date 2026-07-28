"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Archive,
  Ban,
  CheckCircle2,
  LogIn,
  Pencil,
  Plus,
  RotateCcw,
  Trash2,
} from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { DataTable, type Column } from "@/components/data-table";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { OnboardTenantDialog } from "@/components/platform/onboard-tenant-dialog";
import { EditTenantDialog } from "@/components/platform/edit-tenant-dialog";
import { ConfirmDialog, useConfirm } from "@/components/confirm-dialog";
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

type Bucket = "active" | "suspended" | "archived" | "deleted";

/** Which lifecycle bucket a tenant falls into — soft-delete wins over status. */
function bucketOf(t: InsuranceCompanyResponse): Bucket {
  if (t.isDeleted) return "deleted";
  if (t.status === "SUSPENDED") return "suspended";
  if (t.status === "ARCHIVED") return "archived";
  return "active"; // ACTIVE + ONBOARDING
}

const TABS: { key: Bucket; label: string }[] = [
  { key: "active", label: "Active" },
  { key: "suspended", label: "Suspended" },
  { key: "archived", label: "Archived" },
  { key: "deleted", label: "Deleted" },
];

export default function PlatformTenantsPage() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { data, loading, error, refetch } = useAsync(() => platformService.tenants(), []);

  const [tab, setTab] = useState<Bucket>("active");
  const [onboardOpen, setOnboardOpen] = useState(false);
  const [editing, setEditing] = useState<InsuranceCompanyResponse | null>(null);
  const del = useConfirm<InsuranceCompanyResponse>();

  const setStatus = useMutation(
    (args: { id: number; status: string }) => platformService.setStatus(args.id, args.status),
    { successMessage: "Tenant updated", onSuccess: () => refetch() },
  );
  const restore = useMutation((id: number) => platformService.restore(id), {
    successMessage: "Tenant restored",
    onSuccess: () => refetch(),
  });

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

  const buckets = useMemo(() => {
    const map: Record<Bucket, InsuranceCompanyResponse[]> = {
      active: [],
      suspended: [],
      archived: [],
      deleted: [],
    };
    (data ?? []).forEach((t) => map[bucketOf(t)].push(t));
    return map;
  }, [data]);

  /** A compact icon button with a tooltip — every tenant action is one of these. */
  const Action = ({
    label,
    onClick,
    disabled,
    destructive,
    children,
  }: {
    label: string;
    onClick: () => void;
    disabled?: boolean;
    destructive?: boolean;
    children: React.ReactNode;
  }) => (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          variant="outline"
          size="icon"
          className={`h-8 w-8 cursor-pointer ${destructive ? "text-destructive hover:text-destructive" : ""}`}
          onClick={onClick}
          disabled={disabled}
          aria-label={label}
        >
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  );

  const columns: Column<InsuranceCompanyResponse>[] = [
    { header: "Tenant", cell: (t) => <span className="font-medium">{t.name}</span> },
    { header: "Code", cell: (t) => t.code },
    { header: "Plan", cell: (t) => optionLabel(String(t.subscriptionPlan)) },
    { header: "Created", cell: (t) => formatDate(t.createdAt) },
    {
      header: "Status",
      cell: (t) => (
        <StatusBadge
          value={t.isDeleted ? "DELETED" : String(t.status)}
          map={{ ...GENERIC_STATUS_META, DELETED: { label: "Deleted", intent: "danger" } }}
        />
      ),
    },
    {
      header: "",
      headerClassName: "w-64",
      cell: (t) => {
        const b = bucketOf(t);
        return (
          <div className="flex justify-end gap-1.5">
            {b === "deleted" ? (
              <Action label="Restore tenant" onClick={() => restore.run(t.id)} disabled={restore.loading}>
                <RotateCcw className="h-4 w-4" />
              </Action>
            ) : (
              <>
                <Action label="Edit tenant" onClick={() => setEditing(t)}>
                  <Pencil className="h-4 w-4" />
                </Action>

                {b === "active" ? (
                  <Action
                    label="Suspend tenant"
                    onClick={() => setStatus.run({ id: t.id, status: "SUSPENDED" })}
                    disabled={setStatus.loading}
                  >
                    <Ban className="h-4 w-4" />
                  </Action>
                ) : (
                  <Action
                    label="Activate tenant"
                    onClick={() => setStatus.run({ id: t.id, status: "ACTIVE" })}
                    disabled={setStatus.loading}
                  >
                    <CheckCircle2 className="h-4 w-4" />
                  </Action>
                )}

                {b !== "archived" && (
                  <Action
                    label="Archive tenant"
                    onClick={() => setStatus.run({ id: t.id, status: "ARCHIVED" })}
                    disabled={setStatus.loading}
                  >
                    <Archive className="h-4 w-4" />
                  </Action>
                )}

                {b !== "archived" && (
                  <Action
                    label="Open workspace (impersonate)"
                    onClick={() => openWorkspace(t)}
                    disabled={entering === t.id}
                  >
                    <LogIn className="h-4 w-4" />
                  </Action>
                )}

                <Action label="Delete tenant" destructive onClick={() => del.ask(t)}>
                  <Trash2 className="h-4 w-4" />
                </Action>
              </>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <TooltipProvider delayDuration={200}>
      <PageHeader
        title="Tenants"
        description="Every insurance company on the platform. Open a workspace to act as that tenant."
        actions={
          <Tooltip>
            <TooltipTrigger asChild>
              <Button className="cursor-pointer" onClick={() => setOnboardOpen(true)}>
                <Plus className="mr-1 h-4 w-4" /> Onboard tenant
              </Button>
            </TooltipTrigger>
            <TooltipContent>Create a new tenant</TooltipContent>
          </Tooltip>
        }
      />

      <Tabs value={tab} onValueChange={(v) => setTab(v as Bucket)}>
        <TabsList>
          {TABS.map((t) => (
            <TabsTrigger key={t.key} value={t.key} className="cursor-pointer">
              {t.label}
              <span className="text-muted-foreground ml-1.5 text-xs">({buckets[t.key].length})</span>
            </TabsTrigger>
          ))}
        </TabsList>

        {TABS.map((t) => (
          <TabsContent key={t.key} value={t.key} className="mt-4">
            <DataState
              loading={loading}
              error={error}
              data={buckets[t.key]}
              emptyWhen={(d) => d.length === 0}
              empty={<EmptyState title={`No ${t.label.toLowerCase()} tenants`} />}
            >
              {(rows) => <DataTable columns={columns} rows={rows} getKey={(x) => x.id} />}
            </DataState>
          </TabsContent>
        ))}
      </Tabs>

      <OnboardTenantDialog open={onboardOpen} onOpenChange={setOnboardOpen} onSaved={refetch} />
      <EditTenantDialog
        tenant={editing}
        open={editing !== null}
        onOpenChange={(v) => !v && setEditing(null)}
        onSaved={refetch}
      />
      <ConfirmDialog
        open={del.open}
        onOpenChange={(v) => !v && del.close()}
        title={`Delete ${del.target?.name}?`}
        description="The tenant is soft-deleted and can be restored from the Deleted tab."
        confirmLabel="Delete tenant"
        onConfirm={() => platformService.remove(del.target!.id)}
        onDone={() => {
          del.close();
          refetch();
        }}
      />
    </TooltipProvider>
  );
}
