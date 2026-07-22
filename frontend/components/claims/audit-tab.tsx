"use client";

import { History } from "lucide-react";
import { DataState, EmptyState } from "@/components/data-state";
import { useAsync } from "@/hooks/useAsync";
import { auditService } from "@/services/miscService";
import { optionLabel } from "@/lib/enums";
import { formatDateTime } from "@/lib/dayjs";

export function AuditTab({ claimId }: { claimId: number }) {
  const { data, loading, error } = useAsync(() => auditService.forClaim(claimId), [claimId]);

  return (
    <DataState
      loading={loading}
      error={error}
      data={data}
      emptyWhen={(d) => d.length === 0}
      empty={
        <EmptyState
          title="No audit entries"
          description="Actions on this claim will be logged here."
        />
      }
    >
      {(entries) => (
        <ol className="relative space-y-6 border-l pl-6">
          {entries.map((e) => (
            <li key={e.id} className="relative">
              <span className="bg-background absolute -left-[27px] flex h-5 w-5 items-center justify-center rounded-full border">
                <History className="h-3 w-3" />
              </span>
              <p className="text-sm font-medium">{optionLabel(e.action)}</p>
              <p className="text-muted-foreground text-xs">
                {optionLabel(e.entityType)} #{e.entityId} · by user {e.userId} ·{" "}
                {formatDateTime(e.createdAt)}
              </p>
            </li>
          ))}
        </ol>
      )}
    </DataState>
  );
}
