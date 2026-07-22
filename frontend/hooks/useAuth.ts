"use client";

import { useMemo } from "react";
import { useAppSelector } from "./redux";
import type { Permission } from "@/lib/permissions";

/** Read-only view of the authenticated user plus permission helpers. */
export function useAuth() {
  const user = useAppSelector((s) => s.auth.user);
  const status = useAppSelector((s) => s.auth.status);
  const impersonatingTenant = useAppSelector((s) => s.auth.impersonatingTenant);

  const permissionSet = useMemo(() => new Set(user?.permissions ?? []), [user]);

  return useMemo(
    () => ({
      user,
      status,
      impersonatingTenant,
      isAuthenticated: status === "authenticated",
      permissions: permissionSet,
      has: (p: Permission | string) => permissionSet.has(p),
      hasAny: (ps: (Permission | string)[]) => ps.some((p) => permissionSet.has(p)),
      hasAll: (ps: (Permission | string)[]) => ps.every((p) => permissionSet.has(p)),
    }),
    [user, status, impersonatingTenant, permissionSet],
  );
}
