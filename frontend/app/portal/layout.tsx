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
import { CustomerAssistant } from "@/components/ai/customer-assistant";
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
    if (status === "unauthenticated") router.replace("/sign-in");
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
        <div className="mx-auto flex h-14 w-full max-w-5xl items-center gap-2 px-3 sm:gap-4 sm:px-4">
          <Link href="/portal" className="flex shrink-0 items-center gap-2">
            <span className="bg-primary text-primary-foreground flex h-8 w-8 items-center justify-center rounded-lg">
              <ShieldCheck className="h-5 w-5" />
            </span>
            {/* Brand text is dropped below sm so the nav + account fit a phone width. */}
            <span className="hidden font-semibold sm:inline">
              ClaimLens <span className="text-muted-foreground font-normal">Portal</span>
            </span>
          </Link>
          <nav className="flex items-center gap-1 sm:ml-4">
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
                    "rounded-md px-2 py-1.5 text-sm font-medium whitespace-nowrap transition-colors sm:px-3",
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
          <div className="ml-auto flex shrink-0 items-center gap-2">
            <ThemeToggle />
            <UserMenu />
          </div>
        </div>
      </header>
      <main className="w-full min-w-0 flex-1 p-4 md:p-6">
        <div className="mx-auto w-full max-w-5xl space-y-6">{children}</div>
      </main>
      <CustomerAssistant />
      <AppFooter />
    </div>
  );
}
