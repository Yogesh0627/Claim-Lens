"use client";

import { useState } from "react";
import { Pencil } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { DataState, EmptyState } from "@/components/data-state";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Can } from "@/components/can";
import { RolePermissionsDialog } from "@/components/users/role-permissions-dialog";
import { useAsync } from "@/hooks/useAsync";
import { roleService } from "@/services/miscService";
import { PERMISSIONS } from "@/lib/permissions";
import type { PermissionSummary, RoleWithPermissions } from "@/lib/types";

// The platform-admin role is the cross-tenant god role — not customizable per tenant.
const LOCKED_ROLE = "PLATFORM_ADMIN";

function byModule(permissions: PermissionSummary[]): [string, PermissionSummary[]][] {
  const map = new Map<string, PermissionSummary[]>();
  for (const p of permissions) {
    const list = map.get(p.module) ?? [];
    list.push(p);
    map.set(p.module, list);
  }
  return [...map.entries()];
}

export default function RolesPage() {
  const { data, loading, error, refetch } = useAsync(() => roleService.catalog(), []);
  const { data: allPermissions } = useAsync(() => roleService.permissionCatalog(), []);
  const [editing, setEditing] = useState<RoleWithPermissions | null>(null);

  return (
    <>
      <PageHeader
        title="Roles & permissions"
        description="Each role and the permissions it grants for your company. Customize a role and it applies to everyone here with it — on their next request, without affecting other tenants."
      />
      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={<EmptyState title="No roles" />}
      >
        {(roles) => (
          <div className="grid gap-4 md:grid-cols-2">
            {roles.map((role) => (
              <Card key={role.id}>
                <CardHeader className="pb-3">
                  <CardTitle className="flex items-center justify-between gap-2 text-base">
                    <span className="flex items-center gap-2">
                      {role.name}
                      {role.customized ? (
                        <span className="rounded bg-blue-500/10 px-1.5 py-0.5 text-[10px] font-medium text-blue-600 dark:text-blue-400">
                          Customized
                        </span>
                      ) : null}
                    </span>
                    {role.code !== LOCKED_ROLE ? (
                      <Can permission={PERMISSIONS.USER_WRITE}>
                        <Button variant="outline" size="sm" onClick={() => setEditing(role)}>
                          <Pencil className="mr-1 h-4 w-4" /> Edit
                        </Button>
                      </Can>
                    ) : (
                      <span className="text-muted-foreground text-xs">locked</span>
                    )}
                  </CardTitle>
                  {role.description ? (
                    <p className="text-muted-foreground text-sm">{role.description}</p>
                  ) : null}
                </CardHeader>
                <CardContent>
                  {role.permissions.length === 0 ? (
                    <p className="text-muted-foreground text-sm">No permissions granted.</p>
                  ) : (
                    <div className="space-y-3">
                      {byModule(role.permissions).map(([module, perms]) => (
                        <div key={module}>
                          <p className="text-muted-foreground mb-1 text-[11px] font-semibold tracking-wide uppercase">
                            {module}
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {perms.map((p) => (
                              <span
                                key={p.code}
                                title={p.code}
                                className="bg-muted rounded px-1.5 py-0.5 text-xs"
                              >
                                {p.name}
                              </span>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </DataState>

      <RolePermissionsDialog
        role={editing}
        allPermissions={allPermissions ?? []}
        open={editing !== null}
        onOpenChange={(v) => !v && setEditing(null)}
        onSaved={refetch}
      />
    </>
  );
}
