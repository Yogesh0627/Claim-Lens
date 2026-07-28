import { http } from "@/lib/http";
import type {
  ApiResponse,
  CreateProductRequest,
  CreateProductVersionRequest,
  ProductDocumentResponse,
  ProductResponse,
  ProductVersionResponse,
  UpdateProductRequest,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const productService = {
  list: () => unwrap(http.get<ApiResponse<ProductResponse[]>>("/products")),
  get: (id: number) => unwrap(http.get<ApiResponse<ProductResponse>>(`/products/${id}`)),
  create: (body: CreateProductRequest) =>
    unwrap(http.post<ApiResponse<ProductResponse>>("/products", body)),
  update: (id: number, body: UpdateProductRequest) =>
    unwrap(http.put<ApiResponse<ProductResponse>>(`/products/${id}`, body)),

  listVersions: (productId: number) =>
    unwrap(http.get<ApiResponse<ProductVersionResponse[]>>(`/products/${productId}/versions`)),
  createVersion: (productId: number, body: CreateProductVersionRequest) =>
    unwrap(http.post<ApiResponse<ProductVersionResponse>>(`/products/${productId}/versions`, body)),
  activateVersion: (productId: number, versionId: number) =>
    unwrap(
      http.post<ApiResponse<ProductVersionResponse>>(
        `/products/${productId}/versions/${versionId}/activate`,
        {},
      ),
    ),

  // ── Product-version documents (e.g. the policy-wording PDF) ──
  listDocuments: (productId: number, versionId: number) =>
    unwrap(
      http.get<ApiResponse<ProductDocumentResponse[]>>(
        `/products/${productId}/versions/${versionId}/documents`,
      ),
    ),
  uploadDocument: (productId: number, versionId: number, file: File, documentType = "POLICY_WORDING") => {
    const form = new FormData();
    form.append("file", file);
    form.append("documentType", documentType);
    return unwrap(
      http.post<ApiResponse<ProductDocumentResponse>>(
        `/products/${productId}/versions/${versionId}/documents`,
        form,
      ),
    );
  },
  /** Fetches the raw bytes and returns a blob URL (revoke it when done). */
  documentDownloadUrl: async (productId: number, versionId: number, documentId: number): Promise<string> => {
    const res = await http.get(
      `/products/${productId}/versions/${versionId}/documents/${documentId}/download`,
      { responseType: "blob" },
    );
    return URL.createObjectURL(res.data as Blob);
  },
};
