import { http } from "@/lib/http";
import type {
  ApiResponse,
  ChangePasswordRequest,
  ProfileResponse,
  UpdateProfileRequest,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

/** The signed-in user's own profile — available to every role. */
export const profileService = {
  me: () => unwrap(http.get<ApiResponse<ProfileResponse>>("/profile")),

  update: (body: UpdateProfileRequest) =>
    unwrap(http.put<ApiResponse<ProfileResponse>>("/profile", body)),

  changePassword: (body: ChangePasswordRequest) =>
    http.post("/profile/change-password", body).then(() => undefined),
};
