import { http } from "@/lib/http";
import type {
  ApiResponse,
  CreateProductRequest,
  CreateProductVersionRequest,
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
};
