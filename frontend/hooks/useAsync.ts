"use client";

import { useCallback, useEffect, useState } from "react";
import { isApiError } from "@/lib/http";

interface AsyncState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/**
 * Minimal data-fetching hook: runs `fn` on mount and whenever a dep changes, exposing
 * loading/error/data plus a manual refetch. We deliberately keep this small instead of pulling in a
 * query library — server state that must live app-wide (auth) goes through Redux instead.
 */
export function useAsync<T>(
  fn: () => Promise<T>,
  deps: unknown[] = [],
  options: { enabled?: boolean } = {},
): AsyncState<T> {
  const enabled = options.enabled ?? true;
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState<boolean>(enabled);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  const refetch = useCallback(() => setTick((t) => t + 1), []);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }
    let active = true;
    setLoading(true);
    setError(null);
    fn()
      .then((result) => {
        if (active) setData(result);
      })
      .catch((e: unknown) => {
        if (active) setError(isApiError(e) ? e.message : "Failed to load");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick, enabled]);

  return { data, loading, error, refetch };
}
