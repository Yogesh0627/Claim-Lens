import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { authService } from "@/services/authService";
import { platformService } from "@/services/platformService";
import {
  clearTokens,
  enterImpersonation,
  exitImpersonation,
  getImpersonationTenant,
  getTokens,
  setTokens,
} from "@/lib/tokenStorage";
import type { LoginRequest, MeResponse } from "@/lib/types";

export type AuthStatus = "idle" | "loading" | "authenticated" | "unauthenticated";

interface AuthState {
  user: MeResponse | null;
  status: AuthStatus;
  error: string | null;
  /** When a platform admin is impersonating a tenant, the tenant's name; otherwise null. */
  impersonatingTenant: string | null;
}

const initialState: AuthState = {
  user: null,
  status: "idle",
  error: null,
  impersonatingTenant: null,
};

/** On app load: if we hold tokens, resolve the current user; otherwise we're logged out. */
export const bootstrapAuth = createAsyncThunk("auth/bootstrap", async () => {
  if (!getTokens()) return null;
  return authService.me();
});

export const login = createAsyncThunk("auth/login", async (body: LoginRequest) => {
  const tokens = await authService.login(body);
  setTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
  return authService.me();
});

export const loginWithGoogle = createAsyncThunk("auth/google", async (credential: string) => {
  const tokens = await authService.google(credential);
  setTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
  return authService.me();
});

/** Platform admin enters a tenant's workspace: get a tenant-scoped token, then resolve the new identity. */
export const enterTenant = createAsyncThunk(
  "auth/enterTenant",
  async (tenant: { id: number; name: string }) => {
    const imp = await platformService.impersonate(tenant.id);
    enterImpersonation(imp.accessToken, imp.tenantName);
    const me = await authService.me();
    return { me, tenantName: imp.tenantName };
  },
);

/** Exit impersonation: restore the platform admin's own identity. */
export const exitTenant = createAsyncThunk("auth/exitTenant", async () => {
  exitImpersonation();
  return authService.me();
});

export const logout = createAsyncThunk("auth/logout", async () => {
  const tokens = getTokens();
  if (tokens) {
    try {
      await authService.logout(tokens.refreshToken);
    } catch {
      // best-effort server-side revocation; local logout proceeds regardless.
    }
  }
  clearTokens();
});

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(bootstrapAuth.pending, (state) => {
        state.status = "loading";
      })
      .addCase(bootstrapAuth.fulfilled, (state, action) => {
        state.user = action.payload;
        state.status = action.payload ? "authenticated" : "unauthenticated";
        state.impersonatingTenant = getImpersonationTenant();
      })
      .addCase(bootstrapAuth.rejected, (state) => {
        state.user = null;
        state.status = "unauthenticated";
      })
      .addCase(login.pending, (state) => {
        state.status = "loading";
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.user = action.payload;
        state.status = "authenticated";
      })
      .addCase(login.rejected, (state, action) => {
        state.status = "unauthenticated";
        state.error = action.error.message ?? "Login failed";
      })
      .addCase(loginWithGoogle.fulfilled, (state, action) => {
        state.user = action.payload;
        state.status = "authenticated";
      })
      .addCase(enterTenant.fulfilled, (state, action) => {
        state.user = action.payload.me;
        state.status = "authenticated";
        state.impersonatingTenant = action.payload.tenantName;
      })
      .addCase(exitTenant.fulfilled, (state, action) => {
        state.user = action.payload;
        state.status = "authenticated";
        state.impersonatingTenant = null;
      })
      .addCase(logout.fulfilled, (state) => {
        state.user = null;
        state.status = "unauthenticated";
      });
  },
});

export default authSlice.reducer;
