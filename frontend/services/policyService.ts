import { http } from "@/lib/http";
import type {
  ApiResponse,
  CreatePolicyRequest,
  PagedResponse,
  PageParams,
  PolicyResponse,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const policyService = {
  list: (params: PageParams = {}) =>
    unwrap(http.get<ApiResponse<PagedResponse<PolicyResponse>>>("/policies", { params })),
  /** All policies (unpaged) — for the policy picker on the new-claim form. */
  options: () => unwrap(http.get<ApiResponse<PolicyResponse[]>>("/policies/options")),
  get: (id: number) => unwrap(http.get<ApiResponse<PolicyResponse>>(`/policies/${id}`)),
  create: (body: CreatePolicyRequest) =>
    unwrap(http.post<ApiResponse<PolicyResponse>>("/policies", body)),
  cancel: (id: number) =>
    unwrap(http.post<ApiResponse<PolicyResponse>>(`/policies/${id}/cancel`, {})),
};
