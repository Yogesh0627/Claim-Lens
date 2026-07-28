import { http } from "@/lib/http";
import type {
  ApiResponse,
  AuditEntryResponse,
  DashboardResponse,
  NotificationResponse,
  PermissionSummary,
  RoleResponse,
  RoleWithPermissions,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const auditService = {
  forClaim: (claimId: number) =>
    unwrap(http.get<ApiResponse<AuditEntryResponse[]>>(`/claims/${claimId}/audit`)),
};

export const notificationService = {
  list: () => unwrap(http.get<ApiResponse<NotificationResponse[]>>("/notifications")),
  markRead: (id: number) => http.post(`/notifications/${id}/read`, {}).then(() => undefined),
};

// Lightweight cross-component signal: the notifications page and the top-bar bell each hold their own
// copy of the list, so marking one read must tell the other to refresh. Avoids a store for one badge.
const NOTIFICATIONS_CHANGED = "notifications:changed";

export function notificationsChanged() {
  if (typeof window !== "undefined") window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED));
}

export function onNotificationsChanged(callback: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  window.addEventListener(NOTIFICATIONS_CHANGED, callback);
  return () => window.removeEventListener(NOTIFICATIONS_CHANGED, callback);
}

export const analyticsService = {
  dashboard: () => unwrap(http.get<ApiResponse<DashboardResponse>>("/analytics/dashboard")),
};

export const roleService = {
  list: () => unwrap(http.get<ApiResponse<RoleResponse[]>>("/roles")),
  get: (id: number) => unwrap(http.get<ApiResponse<RoleResponse>>(`/roles/${id}`)),
  /** Every role with the permissions it grants (read-only roles-and-permissions view). */
  catalog: () => unwrap(http.get<ApiResponse<RoleWithPermissions[]>>("/roles/catalog")),
  /** Every permission in the system, for the editor's checkboxes. */
  permissionCatalog: () =>
    unwrap(http.get<ApiResponse<PermissionSummary[]>>("/roles/permission-catalog")),
  /** Replace a role's granted permissions (platform-admin only). */
  setPermissions: (roleId: number, permissionCodes: string[]) =>
    unwrap(http.put<ApiResponse<null>>(`/roles/${roleId}/permissions`, { permissionCodes })),
};
