"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight } from "lucide-react";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
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
    // h-full + flex so the card matches the sign-in form's height and the password hint can sit
    // against the bottom edge rather than leaving a gap under it.
    <Card className="flex h-full w-full flex-col">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm">Explore as… — demo accounts</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-1 flex-col">
        {/* Two columns: eight roles stacked vertically made the page scroll. A 2x4 grid halves the
            height so the whole sign-in view fits on screen. Single column on very narrow phones,
            where two would truncate the role names to uselessness. */}
        <TooltipProvider delayDuration={200}>
          <div className="grid grid-cols-1 gap-2 min-[380px]:grid-cols-2">
            {ACCOUNTS.map((a) => (
              <Tooltip key={a.email}>
                <TooltipTrigger asChild>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => signIn(a.email)}
                    disabled={busy !== null}
                    className="group h-auto w-full cursor-pointer flex-col items-start gap-0.5 px-3 py-2 text-left font-normal"
                  >
                    <span className="flex w-full items-center gap-1">
                      <span className="truncate text-xs font-medium">{a.role}</span>
                      <ArrowRight className="text-muted-foreground group-hover:text-foreground ml-auto h-3 w-3 shrink-0" />
                    </span>
                    <span className="text-muted-foreground w-full truncate text-[11px] font-normal">
                      {busy === a.email ? "Signing in…" : a.desc}
                    </span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Sign in as {a.role}</TooltipContent>
              </Tooltip>
            ))}
          </div>
        </TooltipProvider>
        <p className="text-muted-foreground mt-auto pt-3 text-xs">
          Sandbox accounts · password{" "}
          <code className="bg-muted rounded px-1 py-0.5">{DEMO_PASSWORD}</code>
        </p>
      </CardContent>
    </Card>
  );
}
