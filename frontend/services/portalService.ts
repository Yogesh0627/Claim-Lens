import { http } from "@/lib/http";
import type {
  ApiResponse,
  ClaimResponse,
  DocumentResponse,
  FileClaimRequest,
  PolicyResponse,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

/** Customer self-service. Every call is ownership-scoped server-side to the logged-in policyholder. */
export const portalService = {
  myPolicies: () => unwrap(http.get<ApiResponse<PolicyResponse[]>>("/portal/policies")),

  myClaims: () => unwrap(http.get<ApiResponse<ClaimResponse[]>>("/portal/claims")),
  myClaim: (id: number) => unwrap(http.get<ApiResponse<ClaimResponse>>(`/portal/claims/${id}`)),

  fileClaim: (body: FileClaimRequest) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>("/portal/claims", body)),
  submitClaim: (id: number) =>
    unwrap(http.post<ApiResponse<ClaimResponse>>(`/portal/claims/${id}/submit`, {})),

  listDocuments: (claimId: number) =>
    unwrap(http.get<ApiResponse<DocumentResponse[]>>(`/portal/claims/${claimId}/documents`)),
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
};
