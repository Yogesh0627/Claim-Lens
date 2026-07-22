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
          <div className="space-y-2">
            {claims.map((c) => (
              <Link key={c.id} href={`/portal/claims/${c.id}`}>
                <Card className="flex items-center justify-between gap-4 p-4 transition-colors hover:bg-muted/50">
                  <div className="min-w-0">
                    <p className="font-medium">{c.claimNumber}</p>
                    <p className="text-muted-foreground text-xs">
                      Incident {formatDate(c.incidentDate)}
                      {c.claimAmount != null ? ` · ${formatCurrency(c.claimAmount, "INR")}` : ""}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-3">
                    <StatusBadge value={c.status} map={CLAIM_STATUS_META} />
                    <ChevronRight className="text-muted-foreground h-4 w-4" />
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
