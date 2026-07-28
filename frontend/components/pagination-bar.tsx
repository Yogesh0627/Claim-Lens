"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { PagedResponse } from "@/lib/types";

interface PaginationBarProps {
  meta: PagedResponse<unknown>;
  onPageChange: (page: number) => void;
  /** Noun for the count label, e.g. "claims", "users". */
  label?: string;
}

/**
 * The row of paging controls under a list. Shows the current window ("Showing 11–20 of 42 claims")
 * and Prev/Next, disabled at the ends. Page numbers are 0-based on the wire but shown 1-based here.
 * Hidden entirely for a single page — nothing to navigate.
 */
export function PaginationBar({ meta, onPageChange, label = "items" }: PaginationBarProps) {
  if (meta.totalPages <= 1) return null;

  const from = meta.totalElements === 0 ? 0 : meta.page * meta.size + 1;
  const to = meta.page * meta.size + meta.content.length;

  return (
    <div className="flex flex-col items-center justify-between gap-3 sm:flex-row">
      <p className="text-muted-foreground text-sm tabular-nums">
        Showing {from}–{to} of {meta.totalElements} {label}
      </p>
      <div className="flex items-center gap-2">
        <span className="text-muted-foreground mr-1 text-sm tabular-nums">
          Page {meta.page + 1} of {meta.totalPages}
        </span>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(meta.page - 1)}
          disabled={meta.first}
        >
          <ChevronLeft className="mr-1 h-4 w-4" /> Prev
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(meta.page + 1)}
          disabled={meta.last}
        >
          Next <ChevronRight className="ml-1 h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
