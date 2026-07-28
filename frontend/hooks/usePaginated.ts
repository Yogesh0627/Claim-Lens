"use client";

import { useEffect, useState } from "react";
import { useAsync } from "./useAsync";
import type { PagedResponse } from "@/lib/types";

/**
 * Drives a server-paginated list. Owns the current page, fetches through {@link useAsync}, and — the
 * important part — keeps the page in range when the underlying data shrinks.
 *
 * The classic bug this prevents: you're on page 2 (showing the 11th row), you delete it, and the app
 * is left pointing at a page that no longer exists — a blank table. After any refetch we compare the
 * current page against `totalPages` and step back to the last page that still has rows. That single
 * guard covers deleting the last row on a page, deleting everything, and any out-of-range page a
 * client might request directly. It can't loop: it only fires while `page` is strictly past the last
 * index, and it always targets a valid page.
 *
 * On a mutation (create/delete/edit), call `refetch()` — the hook refetches the current page and, if
 * that page has since emptied, clamps automatically.
 */
export function usePaginated<T>(
  fetcher: (params: { page: number; size: number }) => Promise<PagedResponse<T>>,
  deps: unknown[] = [],
  options: { size?: number } = {},
) {
  const size = options.size ?? 10;
  const [page, setPage] = useState(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const state = useAsync(() => fetcher({ page, size }), [page, size, ...deps]);
  const meta = state.data;

  useEffect(() => {
    if (!meta) return;
    const lastIndex = Math.max(0, meta.totalPages - 1);
    if (page > lastIndex) setPage(lastIndex);
  }, [meta, page]);

  return {
    rows: meta?.content ?? [],
    meta,
    page,
    setPage,
    loading: state.loading,
    error: state.error,
    refetch: state.refetch,
  };
}
