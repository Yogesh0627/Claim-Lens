import { http } from "@/lib/http";
import type {
  ApiResponse,
  CreateCustomerRequest,
  CustomerResponse,
  UpdateCustomerRequest,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const customerService = {
  list: () => unwrap(http.get<ApiResponse<CustomerResponse[]>>("/customers")),
  get: (id: number) => unwrap(http.get<ApiResponse<CustomerResponse>>(`/customers/${id}`)),
  create: (body: CreateCustomerRequest) =>
    unwrap(http.post<ApiResponse<CustomerResponse>>("/customers", body)),
  update: (id: number, body: UpdateCustomerRequest) =>
    unwrap(http.put<ApiResponse<CustomerResponse>>(`/customers/${id}`, body)),
  remove: (id: number) => http.delete(`/customers/${id}`).then(() => undefined),
};
