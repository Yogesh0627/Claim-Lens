"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { CheckCircle2, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useMutation } from "@/hooks/useMutation";
import { authService } from "@/services/authService";

/**
 * Landing page for the emailed "set your password" link (invitation or reset). The token in the URL
 * is the credential — there is no session here, and the user may never have had one.
 */
export default function SetPasswordPage() {
  // useSearchParams needs a Suspense boundary, or the production build fails on this route.
  return (
    <Suspense fallback={<div className="bg-muted/40 flex-1" />}>
      <SetPasswordForm />
    </Suspense>
  );
}

function SetPasswordForm() {
  const router = useRouter();
  const token = useSearchParams().get("token") ?? "";
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [done, setDone] = useState(false);

  const mismatch = confirm.length > 0 && password !== confirm;
  const tooShort = password.length > 0 && password.length < 6;
  const canSubmit = !!token && password.length >= 6 && password === confirm;

  const submit = useMutation(() => authService.setPassword(token, password), {
    successMessage: "Password set — you can sign in now",
    onSuccess: () => {
      setDone(true);
      setTimeout(() => router.replace("/sign-in"), 1200);
    },
  });

  return (
    <div className="bg-muted/40 flex flex-1 flex-col">
      <div className="flex flex-1 flex-col items-center justify-center gap-4 p-4 py-8">
        <Card className="w-full max-w-sm">
          <CardHeader className="space-y-1 text-center">
            <div className="bg-primary text-primary-foreground mx-auto mb-2 flex h-11 w-11 items-center justify-center rounded-xl">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <CardTitle className="text-xl">Choose your password</CardTitle>
            <CardDescription>
              {token
                ? "Pick a password for your ClaimLens account."
                : "This link is missing its token."}
            </CardDescription>
          </CardHeader>

          <CardContent>
            {!token ? (
              <p className="text-muted-foreground text-sm">
                Open the link exactly as it appears in your email, or{" "}
                <Link href="/sign-in" className="text-foreground font-medium hover:underline">
                  go to sign in
                </Link>{" "}
                and request a new one.
              </p>
            ) : done ? (
              <div className="flex flex-col items-center gap-2 py-4 text-center">
                <CheckCircle2 className="h-8 w-8 text-emerald-600 dark:text-emerald-400" />
                <p className="text-sm">Password set. Taking you to sign in…</p>
              </div>
            ) : (
              <form
                noValidate
                className="space-y-4"
                onSubmit={(e) => {
                  e.preventDefault();
                  if (canSubmit) submit.run();
                }}
              >
                <div className="space-y-2">
                  <Label htmlFor="password">New password</Label>
                  <Input
                    id="password"
                    type="password"
                    autoComplete="new-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  {tooShort ? (
                    <p className="text-destructive text-xs">Use at least 6 characters.</p>
                  ) : null}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="confirm">Confirm password</Label>
                  <Input
                    id="confirm"
                    type="password"
                    autoComplete="new-password"
                    value={confirm}
                    onChange={(e) => setConfirm(e.target.value)}
                  />
                  {mismatch ? (
                    <p className="text-destructive text-xs">Passwords don&apos;t match.</p>
                  ) : null}
                </div>

                <Button type="submit" className="w-full" disabled={!canSubmit || submit.loading}>
                  {submit.loading ? "Saving…" : "Set password"}
                </Button>

                <p className="text-muted-foreground text-center text-xs">
                  This link can only be used once and expires.
                </p>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
