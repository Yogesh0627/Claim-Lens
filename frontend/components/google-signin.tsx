"use client";

import { GoogleLogin, GoogleOAuthProvider } from "@react-oauth/google";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useAppDispatch } from "@/hooks/redux";
import { loginWithGoogle } from "@/store/authSlice";
import { homePathFor } from "@/lib/navigation";

const CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

/** "Sign in with Google" button. Renders nothing if no client id is configured. */
export function GoogleSignIn() {
  const dispatch = useAppDispatch();
  const router = useRouter();

  if (!CLIENT_ID) return null;

  return (
    <GoogleOAuthProvider clientId={CLIENT_ID}>
      <div className="flex justify-center">
        <GoogleLogin
          text="signin_with"
          shape="rectangular"
          width="320"
          onSuccess={async (resp) => {
            if (!resp.credential) {
              toast.error("Google sign-in returned no credential");
              return;
            }
            try {
              const me = await dispatch(loginWithGoogle(resp.credential)).unwrap();
              router.replace(homePathFor(new Set(me.permissions)));
            } catch (e) {
              // Backend message (e.g. "No ClaimLens account exists for this Google email").
              toast.error(e instanceof Error && e.message ? e.message : "Google sign-in failed");
            }
          }}
          onError={() => toast.error("Google sign-in failed")}
        />
      </div>
    </GoogleOAuthProvider>
  );
}
