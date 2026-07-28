"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChevronRight, Plus } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { DataState, EmptyState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { useAsync } from "@/hooks/useAsync";
import { portalService } from "@/services/portalService";
import { CLAIM_STATUS_META } from "@/lib/enums";
import { formatCurrency } from "@/lib/format";
import { formatDate } from "@/lib/dayjs";

export default function PortalClaimsPage() {
  const router = useRouter();
  const { data, loading, error } = useAsync(() => portalService.myClaims(), []);

  return (
    <>
      <PageHeader
        title="My claims"
        description="File a new claim and track the ones you've already raised."
        actions={
          <Button onClick={() => router.push("/portal/claims/new")}>
            <Plus className="mr-1 h-4 w-4" /> File a claim
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
            title="No claims yet"
            description="When you file a claim it will appear here so you can track its progress."
          />
        }
      >
        {(claims) => (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {claims.map((c) => (
              <Link key={c.id} href={`/portal/claims/${c.id}`} className="group block">
                <Card className="hover:border-primary/40 hover:bg-muted/40 flex h-full flex-col gap-3 p-4 transition-colors">
                  <div className="flex items-start justify-between gap-2">
                    <p className="min-w-0 truncate font-medium">{c.claimNumber}</p>
                    <StatusBadge value={c.status} map={CLAIM_STATUS_META} />
                  </div>

                  <dl className="grid grid-cols-3 gap-y-1 text-sm">
                    <dt className="text-muted-foreground col-span-1">Vehicle</dt>
                    <dd className="col-span-2 text-right font-medium">
                      {c.vehicleRegistrationNumber ?? "—"}
                    </dd>
                    <dt className="text-muted-foreground col-span-1">Incident</dt>
                    <dd className="col-span-2 text-right font-medium">
                      {formatDate(c.incidentDate)}
                    </dd>
                    <dt className="text-muted-foreground col-span-1">Amount</dt>
                    <dd className="col-span-2 text-right font-medium">
                      {c.claimAmount != null ? formatCurrency(c.claimAmount, "INR") : "—"}
                    </dd>
                  </dl>

                  <div className="text-muted-foreground group-hover:text-foreground mt-auto flex items-center justify-end gap-1 text-xs transition-colors">
                    View details <ChevronRight className="h-3.5 w-3.5" />
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}
      </DataState>
    </>
  );
}
