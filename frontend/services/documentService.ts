import { http } from "@/lib/http";
import type { ApiResponse, DocumentResponse, DocumentVersionResponse } from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const documentService = {
  list: (claimId: number) =>
    unwrap(http.get<ApiResponse<DocumentResponse[]>>(`/claims/${claimId}/documents`)),

  upload: (claimId: number, documentType: string, file: File) => {
    const form = new FormData();
    form.append("documentType", documentType);
    form.append("file", file);
    return unwrap(
      http.post<ApiResponse<DocumentResponse>>(`/claims/${claimId}/documents`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      }),
    );
  },

  /** Upload a new revision of an existing document; prior versions are kept. */
  uploadVersion: (claimId: number, documentId: number, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return unwrap(
      http.post<ApiResponse<DocumentResponse>>(
        `/claims/${claimId}/documents/${documentId}/versions`,
        form,
        { headers: { "Content-Type": "multipart/form-data" } },
      ),
    );
  },

  listVersions: (claimId: number, documentId: number) =>
    unwrap(
      http.get<ApiResponse<DocumentVersionResponse[]>>(
        `/claims/${claimId}/documents/${documentId}/versions`,
      ),
    ),

  /** Fetches the raw bytes and returns a blob URL (revoke it when done). */
  async downloadUrl(claimId: number, documentId: number): Promise<string> {
    const res = await http.get(`/claims/${claimId}/documents/${documentId}/download`, {
      responseType: "blob",
    });
    return URL.createObjectURL(res.data as Blob);
  },

  async versionDownloadUrl(
    claimId: number,
    documentId: number,
    versionId: number,
  ): Promise<string> {
    const res = await http.get(
      `/claims/${claimId}/documents/${documentId}/versions/${versionId}/download`,
      { responseType: "blob" },
    );
    return URL.createObjectURL(res.data as Blob);
  },
};
