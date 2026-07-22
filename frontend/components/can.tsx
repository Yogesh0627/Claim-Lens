"use client";

import { useAuth } from "@/hooks/useAuth";
import type { Permission } from "@/lib/permissions";

interface CanProps {
  /** Render children only if the user has (any of) these permissions. */
  permission: Permission | string | (Permission | string)[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

/** Declarative permission gate for UI elements. */
export function Can({ permission, children, fallback = null }: CanProps) {
  const { hasAny } = useAuth();
  const perms = Array.isArray(permission) ? permission : [permission];
  return <>{hasAny(perms) ? children : fallback}</>;
}
