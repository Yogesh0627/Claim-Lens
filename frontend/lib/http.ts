/**
 * The single axios instance every service uses. Responsibilities:
 *  - attach the Bearer access token on each request,
 *  - transparently refresh a 401'd access token once (single-flight) and retry,
 *  - on unrecoverable auth failure, clear tokens and bounce to /sign-in,
 *  - normalise backend errors ({ code, message, errors }) into a typed ApiError.
 *
 * The ApiResponse envelope ({ success, data }) is unwrapped by each service, not here, so callers
 * stay explicitly typed. 204 responses carry no body.
 */
import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";
import {
  clearTokens,
  exitImpersonation,
  getAccessToken,
  getRefreshToken,
  isImpersonating,
  setTokens,
} from "./tokenStorage";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export interface ApiError {
  status: number;
  code: string;
  message: string;
  /** field -> message, present only for VALIDATION_ERROR responses. */
  fieldErrors?: Record<string, string>;
}

interface BackendErrorBody {
  code?: string;
  message?: string;
  errors?: Record<string, string>;
}

export const http: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// A bare client (no interceptors) for the refresh call itself — avoids recursion.
const refreshClient = axios.create({ baseURL: BASE_URL });

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken();
  if (token) {
    const headers = AxiosHeaders.from(config.headers);
    headers.set("Authorization", `Bearer ${token}`);
    config.headers = headers;
  }
  return config;
});

// Single-flight refresh: many parallel 401s share one refresh round-trip.
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error("No refresh token");
  const { data } = await refreshClient.post("/auth/refresh-token", { refreshToken });
  const payload = data?.data;
  if (!payload?.accessToken || !payload?.refreshToken) {
    throw new Error("Malformed refresh response");
  }
  setTokens({ accessToken: payload.accessToken, refreshToken: payload.refreshToken });
  return payload.accessToken as string;
}

function redirectToSignIn(): void {
  clearTokens();
  if (typeof window !== "undefined" && !window.location.pathname.startsWith("/sign-in")) {
    window.location.assign("/sign-in");
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<BackendErrorBody>) => {
    const original = error.config as
      (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const status = error.response?.status ?? 0;
    const url = original?.url ?? "";
    const isAuthCall =
      url.includes("/auth/login") ||
      url.includes("/auth/refresh-token") ||
      url.includes("/auth/google");

    // Impersonation has no refresh token — a 401 means the tenant session expired, so drop back to
    // the platform console under the platform admin's restored identity.
    if (status === 401 && !isAuthCall && isImpersonating()) {
      exitImpersonation();
      if (typeof window !== "undefined") window.location.assign("/platform");
      return Promise.reject({
        status,
        code: "IMPERSONATION_EXPIRED",
        message: "Tenant session expired",
      } satisfies ApiError);
    }

    if (status === 401 && original && !original._retried && !isAuthCall && getRefreshToken()) {
      original._retried = true;
      try {
        refreshPromise = refreshPromise ?? refreshAccessToken();
        const newToken = await refreshPromise;
        refreshPromise = null;
        const headers = AxiosHeaders.from(original.headers);
        headers.set("Authorization", `Bearer ${newToken}`);
        original.headers = headers;
        return http(original);
      } catch {
        refreshPromise = null;
        redirectToSignIn();
      }
    }

    if (status === 401 && !isAuthCall) {
      redirectToSignIn();
    }

    const body = error.response?.data;
    const apiError: ApiError = {
      status,
      code: body?.code ?? "NETWORK_ERROR",
      message: body?.message ?? error.message ?? "Something went wrong",
      fieldErrors: body?.errors,
    };
    return Promise.reject(apiError);
  },
);

/** Type guard so components can narrow caught errors to ApiError. */
export function isApiError(e: unknown): e is ApiError {
  return typeof e === "object" && e !== null && "code" in e && "message" in e;
}
