import { http } from "@/lib/http";
import type { ApiResponse, LoginRequest, LoginResponse, MeResponse } from "@/lib/types";

export const authService = {
  async login(body: LoginRequest): Promise<LoginResponse> {
    const res = await http.post<ApiResponse<LoginResponse>>("/auth/login", body);
    return res.data.data;
  },

  async google(credential: string): Promise<LoginResponse> {
    const res = await http.post<ApiResponse<LoginResponse>>("/auth/google", { credential });
    return res.data.data;
  },

  async me(): Promise<MeResponse> {
    const res = await http.get<ApiResponse<MeResponse>>("/auth/me");
    return res.data.data;
  },

  async logout(refreshToken: string): Promise<void> {
    await http.post("/auth/logout", { refreshToken });
  },

  /** Redeem an invitation or reset link. The token is the credential — no session needed. */
  async setPassword(token: string, password: string): Promise<void> {
    await http.post("/auth/set-password", { token, password });
  },

  /** Always resolves, whether or not the address has an account (no enumeration). */
  async forgotPassword(email: string): Promise<void> {
    await http.post("/auth/forgot-password", { email });
  },
};
