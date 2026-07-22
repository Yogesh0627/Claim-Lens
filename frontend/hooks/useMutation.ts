"use client";

import { useState } from "react";
import { toast } from "sonner";
import { isApiError } from "@/lib/http";

interface MutationOptions<TResult> {
  successMessage?: string;
  onSuccess?: (result: TResult) => void;
}

/** Wraps a service call with loading state + success/error toasts. Re-throws so callers can react. */
export function useMutation<TArgs extends unknown[], TResult>(
  fn: (...args: TArgs) => Promise<TResult>,
  opts: MutationOptions<TResult> = {},
) {
  const [loading, setLoading] = useState(false);

  const run = async (...args: TArgs): Promise<TResult> => {
    setLoading(true);
    try {
      const result = await fn(...args);
      if (opts.successMessage) toast.success(opts.successMessage);
      opts.onSuccess?.(result);
      return result;
    } catch (e) {
      toast.error(isApiError(e) ? e.message : "Action failed");
      throw e;
    } finally {
      setLoading(false);
    }
  };

  return { run, loading };
}
