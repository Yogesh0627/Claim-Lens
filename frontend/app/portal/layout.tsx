"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Loader2, ShieldCheck } from "lucide-react";
import { cn } from "@/lib/utils";
import { ThemeToggle } from "@/components/theme-toggle";
import { UserMenu } from "@/components/user-menu";
import { AppFooter } from "@/components/app-footer";
import { RouteTitle } from "@/components/route-title";
import { useAuth } from "@/hooks/useAuth";
import { homePathFor } from "@/lib/navigation";
import { PERMISSIONS } from "@/lib/permissions";

const NAV = [
  { label: "My claims", href: "/portal" },
  { label: "My policies", href: "/portal/policies" },
];

export default function PortalLayout({ children }: { children: React.ReactNode }) {
  const { status, has, permissions } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "unauthenticated") router.replace("/login");
    else if (status === "authenticated" && !has(PERMISSIONS.PORTAL_CLAIM_READ)) {
      router.replace(homePathFor(permissions));
    }
  }, [status, has, permissions, router]);

  if (status !== "authenticated" || !has(PERMISSIONS.PORTAL_CLAIM_READ)) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="text-muted-foreground h-6 w-6 animate-spin" />
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col">
      <RouteTitle />
      <header className="bg-background/95 supports-[backdrop-filter]:bg-background/60 sticky top-0 z-10 border-b backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-5xl items-center gap-4 px-4">
          <Link href="/portal" className="flex items-center gap-2">
            <span className="bg-primary text-primary-foreground flex h-8 w-8 items-center justify-center rounded-lg">
              <ShieldCheck className="h-5 w-5" />
            </span>
            <span className="font-semibold">
              ClaimLens <span className="text-muted-foreground font-normal">Portal</span>
            </span>
          </Link>
          <nav className="ml-4 flex items-center gap-1">
            {NAV.map((item) => {
              const active =
                item.href === "/portal"
                  ? pathname === "/portal"
                  : pathname.startsWith(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                    active
                      ? "bg-muted text-foreground"
                      : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <UserMenu />
          </div>
        </div>
      </header>
      <main className="w-full min-w-0 flex-1 p-4 md:p-6">
        <div className="mx-auto w-full max-w-5xl space-y-6">{children}</div>
      </main>
      <AppFooter />
    </div>
  );
}
