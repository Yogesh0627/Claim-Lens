"use client";

import { Check } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { useAsync } from "@/hooks/useAsync";
import { notificationService, notificationsChanged } from "@/services/miscService";
import { fromNow } from "@/lib/dayjs";
import { isApiError } from "@/lib/http";

export default function NotificationsPage() {
  const { data, loading, error, refetch } = useAsync(() => notificationService.list(), []);

  const markRead = async (id: number) => {
    try {
      await notificationService.markRead(id);
      refetch();
      // Tell the top-bar bell to refresh its unread badge — it holds its own copy of the list.
      notificationsChanged();
    } catch (e) {
      toast.error(isApiError(e) ? e.message : "Could not update notification");
    }
  };

  return (
    <>
      <PageHeader title="Notifications" description="Updates assigned to you" />
      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={<EmptyState title="No notifications" description="You're all caught up." />}
      >
        {(items) => (
          <div className="space-y-2">
            {items.map((n) => (
              <Card
                key={n.id}
                className={cn(
                  "flex items-start justify-between gap-4 p-4",
                  !n.read && "border-l-primary border-l-4",
                )}
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <p className="font-medium">{n.title}</p>
                    {!n.read ? (
                      <span className="bg-primary h-2 w-2 rounded-full" aria-label="Unread" />
                    ) : null}
                  </div>
                  <p className="text-muted-foreground text-sm">{n.message}</p>
                  <p className="text-muted-foreground text-xs">{fromNow(n.createdAt)}</p>
                </div>
                {!n.read ? (
                  <Button variant="ghost" size="sm" onClick={() => markRead(n.id)}>
                    <Check className="mr-1 h-4 w-4" /> Mark read
                  </Button>
                ) : null}
              </Card>
            ))}
          </div>
        )}
      </DataState>
    </>
  );
}
