"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { GoogleSignIn } from "@/components/google-signin";
import { ForgotPasswordDialog } from "@/components/auth/forgot-password-dialog";
import { DemoAccounts } from "@/components/demo-accounts";
import { useAppDispatch } from "@/hooks/redux";
import { useAuth } from "@/hooks/useAuth";
import { login } from "@/store/authSlice";
import { homePathFor } from "@/lib/navigation";
import { isApiError } from "@/lib/http";

interface LoginForm {
  email: string;
  password: string;
}

export default function LoginPage() {
  const dispatch = useAppDispatch();
  const router = useRouter();
  const { status, permissions } = useAuth();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ defaultValues: { email: "", password: "" } });

  // Already signed in (e.g. navigated back to /sign-in) -> bounce to the app.
  useEffect(() => {
    if (status === "authenticated") {
      router.replace(homePathFor(permissions));
    }
  }, [status, permissions, router]);

  const onSubmit = async (values: LoginForm) => {
    try {
      const me = await dispatch(login(values)).unwrap();
      router.replace(homePathFor(new Set(me.permissions)));
    } catch (e) {
      toast.error(isApiError(e) ? e.message : "Login failed");
    }
  };

  return (
    // flex-1, not min-h-screen: the layout supplies the header and footer, so a full-viewport
    // block here would push the footer below the fold. The theme toggle lives in the header.
    <div className="bg-muted/40 flex flex-1 flex-col">
      {/* Two columns from `lg` up so the form and the demo accounts sit side by side and the page
          fits without scrolling; they stack on narrow screens where side-by-side would squash both.
          The grid's default align-items:stretch gives both cards the same height. */}
      <div className="flex flex-1 items-center justify-center p-4 py-8">
        <div className="grid w-full max-w-4xl justify-items-center gap-6 lg:grid-cols-2 lg:justify-items-stretch">
          <Card className="w-full max-w-sm lg:h-full lg:justify-self-end">
          <CardHeader className="space-y-1 text-center">
            <div className="bg-primary text-primary-foreground mx-auto mb-2 flex h-11 w-11 items-center justify-center rounded-xl">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <CardTitle className="text-xl">ClaimLens</CardTitle>
            <CardDescription>Sign in to the claims platform</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  placeholder="you@company.com"
                  {...register("email", { required: "Email is required" })}
                />
                {errors.email ? (
                  <p className="text-destructive text-xs">{errors.email.message}</p>
                ) : null}
              </div>
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="password">Password</Label>
                  <ForgotPasswordDialog />
                </div>
                <Input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  {...register("password", { required: "Password is required" })}
                />
                {errors.password ? (
                  <p className="text-destructive text-xs">{errors.password.message}</p>
                ) : null}
              </div>
              <Button type="submit" className="w-full" disabled={isSubmitting}>
                {isSubmitting ? "Signing in…" : "Sign in"}
              </Button>
            </form>

            <div className="my-4 flex items-center gap-3">
              <div className="bg-border h-px flex-1" />
              <span className="text-muted-foreground text-xs">or</span>
              <div className="bg-border h-px flex-1" />
            </div>
            <GoogleSignIn />
          </CardContent>
        </Card>

        {/* Wider than the form: it holds a 2x4 grid of roles, the form is a single column. */}
        <div className="w-full max-w-sm lg:h-full lg:max-w-md lg:justify-self-start">
          <DemoAccounts />
        </div>
        </div>
      </div>
    </div>
  );
}
