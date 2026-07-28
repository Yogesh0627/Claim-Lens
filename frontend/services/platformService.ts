import { http } from "@/lib/http";
import type {
  ApiResponse,
  ImpersonationResponse,
  InsuranceCompanyResponse,
  OnboardTenantRequest,
  PlatformAnalyticsResponse,
  UpdateTenantRequest,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const platformService = {
  analytics: () => unwrap(http.get<ApiResponse<PlatformAnalyticsResponse>>("/platform/analytics")),

  tenants: () => unwrap(http.get<ApiResponse<InsuranceCompanyResponse[]>>("/platform/tenants")),

  onboard: (body: OnboardTenantRequest) =>
    unwrap(http.post<ApiResponse<InsuranceCompanyResponse>>("/platform/tenants", body)),

  update: (tenantId: number, body: UpdateTenantRequest) =>
    unwrap(http.put<ApiResponse<InsuranceCompanyResponse>>(`/platform/tenants/${tenantId}`, body)),

  setStatus: (tenantId: number, status: string) =>
    unwrap(
      http.post<ApiResponse<InsuranceCompanyResponse>>(`/platform/tenants/${tenantId}/status`, {
        status,
      }),
    ),

  remove: (tenantId: number) =>
    unwrap(http.delete<ApiResponse<InsuranceCompanyResponse>>(`/platform/tenants/${tenantId}`)),

  restore: (tenantId: number) =>
    unwrap(
      http.post<ApiResponse<InsuranceCompanyResponse>>(`/platform/tenants/${tenantId}/restore`, {}),
    ),

  impersonate: (tenantId: number) =>
    unwrap(
      http.post<ApiResponse<ImpersonationResponse>>(
        `/platform/tenants/${tenantId}/impersonate`,
        {},
      ),
    ),
};
