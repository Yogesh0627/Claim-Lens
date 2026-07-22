"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight } from "lucide-react";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAppDispatch } from "@/hooks/redux";
import { login } from "@/store/authSlice";
import { homePathFor } from "@/lib/navigation";
import { isApiError } from "@/lib/http";

const DEMO_PASSWORD = "Password123!";

const ACCOUNTS = [
  {
    role: "Platform Admin",
    email: "platformadmin@demo.claimlens.app",
    desc: "Cross-tenant SaaS operator",
  },
  { role: "Tenant Admin", email: "admin@demo.claimlens.app", desc: "Runs the insurance company" },
  {
    role: "Investigation Manager",
    email: "manager@demo.claimlens.app",
    desc: "Assigns & oversees claims",
  },
  {
    role: "Investigator",
    email: "investigator@demo.claimlens.app",
    desc: "Investigates an assigned claim",
  },
  {
    role: "Customer (Policyholder)",
    email: "chauhanyogesh950+rahul@gmail.com",
    desc: "Files & tracks their own claims",
  },
  {
    role: "Customer Support",
    email: "support@demo.claimlens.app",
    desc: "Raises claims for customers",
  },
  {
    role: "Employee (Adjuster)",
    email: "employee@demo.claimlens.app",
    desc: "General employee view",
  },
  { role: "Auditor", email: "auditor@demo.claimlens.app", desc: "Audit trails & analytics" },
];

/** Shown on the login page (unless NEXT_PUBLIC_DEMO=false): one-click sign-in as any role. */
export function DemoAccounts() {
  const dispatch = useAppDispatch();
  const router = useRouter();
  const [busy, setBusy] = useState<string | null>(null);

  if (process.env.NEXT_PUBLIC_DEMO === "false") return null;

  const signIn = async (email: string) => {
    setBusy(email);
    try {
      const me = await dispatch(login({ email, password: DEMO_PASSWORD })).unwrap();
      router.replace(homePathFor(new Set(me.permissions)));
    } catch (e) {
      toast.error(isApiError(e) ? e.message : "Demo sign-in failed");
      setBusy(null);
    }
  };

  return (
    <Card className="w-full max-w-sm">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm">Explore as… — demo accounts</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {ACCOUNTS.map((a) => (
          <Button
            key={a.email}
            type="button"
            variant="outline"
            onClick={() => signIn(a.email)}
            disabled={busy !== null}
            className="group h-auto w-full justify-between gap-3 px-3 py-2 text-left font-normal"
          >
            <span className="min-w-0">
              <span className="block text-sm font-medium">{a.role}</span>
              <span className="text-muted-foreground block truncate text-xs font-normal">
                {a.desc}
              </span>
            </span>
            <span className="text-muted-foreground group-hover:text-foreground flex shrink-0 items-center gap-1 text-xs">
              {busy === a.email ? "Signing in…" : "Sign in"}
              <ArrowRight className="h-3 w-3" />
            </span>
          </Button>
        ))}
        <p className="text-muted-foreground pt-1 text-xs">
          Sandbox accounts · password{" "}
          <code className="bg-muted rounded px-1 py-0.5">{DEMO_PASSWORD}</code>
        </p>
      </CardContent>
    </Card>
  );
}
