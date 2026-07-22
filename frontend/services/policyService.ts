import { http } from "@/lib/http";
import type { ApiResponse, CreatePolicyRequest, PolicyResponse } from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const policyService = {
  list: () => unwrap(http.get<ApiResponse<PolicyResponse[]>>("/policies")),
  get: (id: number) => unwrap(http.get<ApiResponse<PolicyResponse>>(`/policies/${id}`)),
  create: (body: CreatePolicyRequest) =>
    unwrap(http.post<ApiResponse<PolicyResponse>>("/policies", body)),
  cancel: (id: number) =>
    unwrap(http.post<ApiResponse<PolicyResponse>>(`/policies/${id}/cancel`, {})),
};
