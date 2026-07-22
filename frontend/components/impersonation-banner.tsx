"use client";

import { useRouter } from "next/navigation";
import { LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAppDispatch } from "@/hooks/redux";
import { useAuth } from "@/hooks/useAuth";
import { exitTenant } from "@/store/authSlice";

/** Shown across the tenant app while a platform admin is impersonating a tenant. */
export function ImpersonationBanner() {
  const { impersonatingTenant } = useAuth();
  const dispatch = useAppDispatch();
  const router = useRouter();

  if (!impersonatingTenant) return null;

  const exit = async () => {
    await dispatch(exitTenant());
    router.push("/platform");
  };

  return (
    <div className="flex flex-wrap items-center justify-center gap-2 bg-amber-500 px-4 py-1.5 text-center text-sm font-medium text-amber-950">
      <span>
        Viewing <b>{impersonatingTenant}</b> as platform admin
      </span>
      <Button size="sm" variant="secondary" className="h-6 px-2 text-xs" onClick={exit}>
        <LogOut className="mr-1 h-3 w-3" /> Exit to console
      </Button>
    </div>
  );
}
