import { http } from "@/lib/http";
import type { ApiResponse, CreateFraudRulesetRequest, FraudRulesetResponse } from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const rulesetService = {
  list: () => unwrap(http.get<ApiResponse<FraudRulesetResponse[]>>("/rulesets/fraud")),
  get: (id: number) => unwrap(http.get<ApiResponse<FraudRulesetResponse>>(`/rulesets/fraud/${id}`)),
  create: (body: CreateFraudRulesetRequest) =>
    unwrap(http.post<ApiResponse<FraudRulesetResponse>>("/rulesets/fraud", body)),
  activate: (id: number) =>
    unwrap(http.post<ApiResponse<FraudRulesetResponse>>(`/rulesets/fraud/${id}/activate`, {})),
};
