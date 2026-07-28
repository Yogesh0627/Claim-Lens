import { http } from "@/lib/http";
import type {
  ApiResponse,
  AssignClaimRequest,
  ClaimDecisionRequest,
  ClaimResponse,
  CreateClaimRequest,
  PagedResponse,
  PageParams,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const claimService = {
  list: (params: PageParams = {}) =>
    unwrap(http.get<ApiResponse<PagedResponse<ClaimResponse>>>("/claims", { params })),
  get: (id: number) => unwrap(http.get<ApiResponse<ClaimResponse>>(`/claims/${id}`)),
  create: (body: CreateClaimRequest) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>("/claims", body)),
  submit: (id: number) => unwrap(http.post<ApiResponse<ClaimResponse>>(`/claims/${id}/submit`, {})),
  assign: (id: number, body: AssignClaimRequest) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>(`/claims/${id}/assign`, body)),
  autoAssign: (id: number) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>(`/claims/${id}/auto-assign`, {})),
  /** Move an under-investigation claim to another investigator; null id = auto-pick. */
  reassign: (id: number, investigatorUserId: number | null) =>
    unwrap(
      http.post<ApiResponse<ClaimResponse>>(`/claims/${id}/reassign`, { investigatorUserId }),
    ),
  decide: (id: number, body: ClaimDecisionRequest) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>(`/claims/${id}/decision`, body)),
  requestInformation: (id: number, message: string) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>(`/claims/${id}/request-information`, { message })),
};
