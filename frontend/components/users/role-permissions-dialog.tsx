"use client";

import { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useMutation } from "@/hooks/useMutation";
import { roleService } from "@/services/miscService";
import type { PermissionSummary, RoleWithPermissions } from "@/lib/types";

// The platform-admin permission is deliberately not grantable through the editor (backend enforces it
// too) — it would be a backdoor to cross-tenant god-mode.
const BLOCKED_PERMISSION = "PLATFORM_ADMIN";

/**
 * Customize which permissions a role grants FOR THIS COMPANY (a per-tenant override — other tenants
 * are unaffected). All permissions shown as checkboxes, grouped by module.
 */
export function RolePermissionsDialog({
  role,
  allPermissions,
  open,
  onOpenChange,
  onSaved,
}: {
  role: RoleWithPermissions | null;
  allPermissions: PermissionSummary[];
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSaved: () => void;
}) {
  const [checked, setChecked] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (role) setChecked(new Set(role.permissions.map((p) => p.code)));
  }, [role]);

  const grouped = useMemo(() => {
    const map = new Map<string, PermissionSummary[]>();
    for (const p of allPermissions) {
      if (p.code === BLOCKED_PERMISSION) continue;
      const list = map.get(p.module) ?? [];
      list.push(p);
      map.set(p.module, list);
    }
    return [...map.entries()];
  }, [allPermissions]);

  const toggle = (code: string, on: boolean) => {
    setChecked((prev) => {
      const next = new Set(prev);
      if (on) next.add(code);
      else next.delete(code);
      return next;
    });
  };

  const save = useMutation(() => roleService.setPermissions(role!.id, [...checked]), {
    successMessage: "Role permissions updated",
    onSuccess: () => {
      onOpenChange(false);
      onSaved();
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Customize permissions — {role?.name}</DialogTitle>
          <DialogDescription>
            Tick the permissions this role grants in your company. Applies to everyone here with this
            role on their next request, and doesn&apos;t affect other tenants. Clearing all resets the
            role to the platform default. {checked.size} selected.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4">
          {grouped.map(([module, perms]) => (
            <div key={module}>
              <p className="text-muted-foreground mb-1.5 text-[11px] font-semibold tracking-wide uppercase">
                {module}
              </p>
              <div className="grid gap-1.5 sm:grid-cols-2">
                {perms.map((p) => (
                  <label
                    key={p.code}
                    className="hover:bg-muted flex cursor-pointer items-center gap-2 rounded px-1.5 py-1"
                    title={p.code}
                  >
                    <Checkbox
                      checked={checked.has(p.code)}
                      onCheckedChange={(c) => toggle(p.code, c === true)}
                    />
                    <span className="text-sm">{p.name}</span>
                  </label>
                ))}
              </div>
            </div>
          ))}
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={() => save.run()} disabled={save.loading}>
            {save.loading ? "Saving…" : "Save permissions"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
