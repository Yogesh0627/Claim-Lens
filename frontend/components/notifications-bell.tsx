"use client";

import Link from "next/link";
import { Bell } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAsync } from "@/hooks/useAsync";
import { notificationService } from "@/services/miscService";

export function NotificationsBell() {
  const { data } = useAsync(() => notificationService.list(), []);
  const unread = (data ?? []).filter((n) => !n.read).length;

  return (
    <Button variant="ghost" size="icon" asChild aria-label="Notifications" className="relative">
      <Link href="/notifications">
        <Bell className="h-5 w-5" />
        {unread > 0 ? (
          <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white">
            {unread > 9 ? "9+" : unread}
          </span>
        ) : null}
      </Link>
    </Button>
  );
}
