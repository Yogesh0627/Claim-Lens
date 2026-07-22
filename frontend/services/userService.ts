import { http } from "@/lib/http";
import type {
  ApiResponse,
  CreateUserRequest,
  UpdateUserRequest,
  UserResponse,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const userService = {
  /** Optionally filter by a single role code, e.g. "INVESTIGATOR". */
  list: (roleCode?: string) =>
    unwrap(
      http.get<ApiResponse<UserResponse[]>>("/users", {
        params: roleCode ? { role: roleCode } : undefined,
      }),
    ),

  create: (body: CreateUserRequest) =>
    unwrap(http.post<ApiResponse<UserResponse>>("/users", body)),

  update: (userId: number, body: UpdateUserRequest) =>
    unwrap(http.put<ApiResponse<UserResponse>>(`/users/${userId}`, body)),
};
