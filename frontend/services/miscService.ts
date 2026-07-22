import { http } from "@/lib/http";
import type {
  ApiResponse,
  AuditEntryResponse,
  DashboardResponse,
  NotificationResponse,
  RoleResponse,
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

export const analyticsService = {
  dashboard: () => unwrap(http.get<ApiResponse<DashboardResponse>>("/analytics/dashboard")),
};

export const roleService = {
  list: () => unwrap(http.get<ApiResponse<RoleResponse[]>>("/roles")),
  get: (id: number) => unwrap(http.get<ApiResponse<RoleResponse>>(`/roles/${id}`)),
};
