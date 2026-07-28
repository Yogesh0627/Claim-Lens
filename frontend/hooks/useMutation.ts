"use client";

import { useState } from "react";
import { toast } from "sonner";
import { isApiError } from "@/lib/http";

interface MutationOptions<TResult> {
  successMessage?: string;
  onSuccess?: (result: TResult) => void;
}

/**
 * Wraps a service call with loading state + success/error toasts. Re-throws so callers can react.
 *
 * On a VALIDATION_ERROR the backend returns a per-field message map; this exposes it as
 * `fieldErrors` so a form can render each message inline under its field, and also surfaces the
 * specific messages in the toast instead of a generic "failed". The map clears on the next run.
 */
export function useMutation<TArgs extends unknown[], TResult>(
  fn: (...args: TArgs) => Promise<TResult>,
  opts: MutationOptions<TResult> = {},
) {
  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string> | undefined>(undefined);

  const run = async (...args: TArgs): Promise<TResult> => {
    setLoading(true);
    setFieldErrors(undefined);
    try {
      const result = await fn(...args);
      if (opts.successMessage) toast.success(opts.successMessage);
      opts.onSuccess?.(result);
      return result;
    } catch (e) {
      if (isApiError(e) && e.fieldErrors && Object.keys(e.fieldErrors).length > 0) {
        setFieldErrors(e.fieldErrors);
        // Show the actual field messages (e.g. "Region name cannot exceed 255 characters"), not a
        // generic "Validation failed" — the same messages also render inline where a form wires them.
        toast.error(Object.values(e.fieldErrors).join(" · "));
      } else {
        toast.error(isApiError(e) ? e.message : "Action failed");
      }
      throw e;
    } finally {
      setLoading(false);
    }
  };

  return { run, loading, fieldErrors };
}
