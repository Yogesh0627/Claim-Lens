"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useMutation } from "@/hooks/useMutation";
import { authService } from "@/services/authService";

/**
 * Request a password-reset link.
 *
 * <p>The confirmation is deliberately vague — "if that address has an account" — and the API returns
 * the same response either way. Saying "no such user" would let anyone test which emails are
 * registered.
 */
export function ForgotPasswordDialog() {
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  const request = useMutation(() => authService.forgotPassword(email), {
    onSuccess: () => setSent(true),
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        setOpen(v);
        if (!v) {
          setSent(false);
          setEmail("");
        }
      }}
    >
      <DialogTrigger asChild>
        <Button type="button" variant="link" className="h-auto p-0 text-xs font-normal">
          Forgot password?
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Reset your password</DialogTitle>
          <DialogDescription>
            We&apos;ll email you a link to choose a new password.
          </DialogDescription>
        </DialogHeader>

        {sent ? (
          <p className="text-sm">
            If <span className="font-medium">{email}</span> has an account, a reset link is on its
            way. It can be used once and expires in an hour.
          </p>
        ) : (
          <div className="grid gap-2">
            <Label htmlFor="reset-email">Email</Label>
            <Input
              id="reset-email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
            />
          </div>
        )}

        <DialogFooter>
          {sent ? (
            <Button type="button" onClick={() => setOpen(false)}>
              Done
            </Button>
          ) : (
            <>
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button
                type="button"
                onClick={() => request.run()}
                disabled={!email.includes("@") || request.loading}
              >
                {request.loading ? "Sending…" : "Send link"}
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
