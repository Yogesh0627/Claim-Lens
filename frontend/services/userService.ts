import { http } from "@/lib/http";
import type {
  ApiResponse,
  CreateUserRequest,
  PagedResponse,
  PageParams,
  UpdateUserRequest,
  UserResponse,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const userService = {
  /** Paged directory listing for the Users screen. */
  list: (params: PageParams = {}) =>
    unwrap(http.get<ApiResponse<PagedResponse<UserResponse>>>("/users", { params })),

  /** All users (unpaged), optionally filtered by role code (e.g. "INVESTIGATOR") — for pickers. */
  options: (roleCode?: string) =>
    unwrap(
      http.get<ApiResponse<UserResponse[]>>("/users/options", {
        params: roleCode ? { role: roleCode } : undefined,
      }),
    ),

  create: (body: CreateUserRequest) =>
    unwrap(http.post<ApiResponse<UserResponse>>("/users", body)),

  update: (userId: number, body: UpdateUserRequest) =>
    unwrap(http.put<ApiResponse<UserResponse>>(`/users/${userId}`, body)),
};
