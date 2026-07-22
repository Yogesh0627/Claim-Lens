"use client";

import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

/** Backend origin + actuator health (health lives at the root, not under /api/v1). */
function healthUrl(): string {
  try {
    return new URL(API_BASE).origin + "/actuator/health";
  } catch {
    return "http://localhost:8080/actuator/health";
  }
}

type Status = "connecting" | "online" | "offline";

/**
 * Cold-start UX + fallback for free-tier hosting (e.g. Render sleeps a service after ~15 min idle).
 * On load it pings the backend — which also *wakes it up* while the visitor reads the page, so by the
 * time they sign in it's warm. Shows a small pill only if the connect is slow (never flashes on a warm
 * or local server), turning amber → green. This is a safety net even if the keep-alive cron lapses.
 */
export function ServerStatus() {
  const [status, setStatus] = useState<Status>("connecting");
  const [visible, setVisible] = useState(false);
  const shownRef = useRef(false);
  const cancelled = useRef(false);

  useEffect(() => {
    cancelled.current = false;
    const url = healthUrl();

    // Only surface the pill if connecting takes a beat — avoids a flash on a warm/local server.
    const showTimer = setTimeout(() => {
      if (!cancelled.current && !shownRef.current) {
        shownRef.current = true;
        setVisible(true);
      }
    }, 1200);

    const ping = async (attempt: number) => {
      if (cancelled.current) return;
      const controller = new AbortController();
      // Render holds the request during a cold start; give it up to ~70s before retrying.
      const t = setTimeout(() => controller.abort(), 70_000);
      try {
        // no-cors: we only care that the server *responded*, not the body — so CORS never blocks it.
        await fetch(url, { mode: "no-cors", cache: "no-store", signal: controller.signal });
        clearTimeout(t);
        if (cancelled.current) return;
        setStatus("online");
        if (shownRef.current) {
          setVisible(true);
          setTimeout(() => !cancelled.current && setVisible(false), 2000); // green flash, then hide
        }
      } catch {
        clearTimeout(t);
        if (cancelled.current) return;
        setStatus("offline");
        shownRef.current = true;
        setVisible(true);
        if (attempt < 40) {
          setTimeout(() => ping(attempt + 1), 3000);
        }
      }
    };
    ping(0);

    return () => {
      cancelled.current = true;
      clearTimeout(showTimer);
    };
  }, []);

  if (!visible) return null;

  const online = status === "online";
  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex justify-center px-4">
      <div
        role="status"
        aria-live="polite"
        className={cn(
          "bg-background/95 pointer-events-auto flex items-center gap-2.5 rounded-full border px-3.5 py-2 text-sm shadow-lg backdrop-blur",
          online && "border-emerald-500/40",
        )}
      >
        {online ? (
          <>
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
            <span className="font-medium text-emerald-600 dark:text-emerald-400">Connected</span>
          </>
        ) : (
          <>
            <span className="relative flex h-2.5 w-2.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-amber-500 opacity-75" />
              <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-amber-500" />
            </span>
            <span className="font-medium">Waking up the server…</span>
            <span className="text-muted-foreground hidden sm:inline">
              free-tier instances sleep when idle — this can take up to a minute ☕
            </span>
          </>
        )}
      </div>
    </div>
  );
}
