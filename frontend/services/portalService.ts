import { http } from "@/lib/http";
import type {
  ApiResponse,
  AskCoverageResponse,
  ClaimResponse,
  ClaimTimelineEntry,
  DocumentResponse,
  DocumentVersionResponse,
  FileClaimRequest,
  PolicyResponse,
  ProductDocumentResponse,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

/** Customer self-service. Every call is ownership-scoped server-side to the logged-in policyholder. */
export const portalService = {
  myPolicies: () => unwrap(http.get<ApiResponse<PolicyResponse[]>>("/portal/policies")),

  policyDocuments: (policyId: number) =>
    unwrap(
      http.get<ApiResponse<ProductDocumentResponse[]>>(`/portal/policies/${policyId}/documents`),
    ),
  async policyDocumentUrl(policyId: number, documentId: number): Promise<string> {
    const res = await http.get(
      `/portal/policies/${policyId}/documents/${documentId}/download`,
      { responseType: "blob" },
    );
    return URL.createObjectURL(res.data as Blob);
  },

  myClaims: () => unwrap(http.get<ApiResponse<ClaimResponse[]>>("/portal/claims")),
  myClaim: (id: number) => unwrap(http.get<ApiResponse<ClaimResponse>>(`/portal/claims/${id}`)),

  fileClaim: (body: FileClaimRequest) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>("/portal/claims", body)),
  submitClaim: (id: number) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>(`/portal/claims/${id}/submit`, {})),

  listDocuments: (claimId: number) =>
    unwrap(http.get<ApiResponse<DocumentResponse[]>>(`/portal/claims/${claimId}/documents`)),

  claimTimeline: (claimId: number) =>
    unwrap(http.get<ApiResponse<ClaimTimelineEntry[]>>(`/portal/claims/${claimId}/timeline`)),

  /** Ask about one of the customer's OWN policies — server resolves the version from the policy. */
  askCoverage: (policyId: number, question: string) =>
    unwrap(
      http.post<ApiResponse<AskCoverageResponse>>("/portal/coverage/ask", { policyId, question }),
    ),
  uploadDocument: (claimId: number, documentType: string, file: File) => {
    const form = new FormData();
    form.append("documentType", documentType);
    form.append("file", file);
    return unwrap(
      http.post<ApiResponse<DocumentResponse>>(`/portal/claims/${claimId}/documents`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      }),
    );
  },
  async downloadUrl(claimId: number, documentId: number): Promise<string> {
    const res = await http.get(`/portal/claims/${claimId}/documents/${documentId}/download`, {
      responseType: "blob",
    });
    return URL.createObjectURL(res.data as Blob);
  },

  documentVersions: (claimId: number, documentId: number) =>
    unwrap(
      http.get<ApiResponse<DocumentVersionResponse[]>>(
        `/portal/claims/${claimId}/documents/${documentId}/versions`,
      ),
    ),
  async documentVersionUrl(
    claimId: number,
    documentId: number,
    versionId: number,
  ): Promise<string> {
    const res = await http.get(
      `/portal/claims/${claimId}/documents/${documentId}/versions/${versionId}/download`,
      { responseType: "blob" },
    );
    return URL.createObjectURL(res.data as Blob);
  },
};
