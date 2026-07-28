import { http } from "@/lib/http";
import type {
  ApiResponse,
  AskableProduct,
  AskCoverageRequest,
  AskCoverageResponse,
  IngestKnowledgeResponse,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const coverageService = {
  ask: (body: AskCoverageRequest) =>
    unwrap(http.post<ApiResponse<AskCoverageResponse>>("/coverage/ask", body)),

  /** Products with ingested wording — the staff assistant's picker. */
  askableProducts: () =>
    unwrap(http.get<ApiResponse<AskableProduct[]>>("/coverage/products")),

  status: (productId: number, versionId: number) =>
    unwrap(
      http.get<ApiResponse<IngestKnowledgeResponse>>(
        `/products/${productId}/versions/${versionId}/knowledge`,
      ),
    ),

  ingest: (productId: number, versionId: number, text: string) =>
    unwrap(
      http.post<ApiResponse<IngestKnowledgeResponse>>(
        `/products/${productId}/versions/${versionId}/knowledge`,
        { text },
      ),
    ),
};
