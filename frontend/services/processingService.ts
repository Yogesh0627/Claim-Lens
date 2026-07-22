import { http } from "@/lib/http";
import type { ApiResponse, ClaimProcessingResponse } from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const processingService = {
  get: (claimId: number) =>
    unwrap(http.get<ApiResponse<ClaimProcessingResponse>>(`/claims/${claimId}/processing`)),
};
