"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useMutation } from "@/hooks/useMutation";

/** Manages confirm-dialog state + the confirmed action's loading/toast. */
export function useConfirm<T>() {
  const [target, setTarget] = useState<T | null>(null);
  return {
    target,
    open: target !== null,
    ask: (t: T) => setTarget(t),
    close: () => setTarget(null),
  };
}

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = "Delete",
  onConfirm,
  onDone,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  title: string;
  description?: string;
  confirmLabel?: string;
  onConfirm: () => Promise<unknown>;
  onDone: () => void;
}) {
  const run = useMutation(onConfirm, {
    successMessage: "Done",
    onSuccess: () => {
      onOpenChange(false);
      onDone();
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          {description ? <DialogDescription>{description}</DialogDescription> : null}
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button variant="destructive" onClick={() => run.run()} disabled={run.loading}>
            {run.loading ? "Working…" : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
