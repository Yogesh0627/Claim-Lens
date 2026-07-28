"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { Separator } from "@/components/ui/separator";
import { AppSidebar } from "@/components/app-sidebar";
import { NotificationsBell } from "@/components/notifications-bell";
import { UserMenu } from "@/components/user-menu";
import { ThemeToggle } from "@/components/theme-toggle";
import { AppFooter } from "@/components/app-footer";
import { ImpersonationBanner } from "@/components/impersonation-banner";
import { RouteTitle } from "@/components/route-title";
import { StaffAssistant } from "@/components/ai/staff-assistant";
import { useAuth } from "@/hooks/useAuth";
import { PERMISSIONS } from "@/lib/permissions";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { status, has } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "unauthenticated") router.replace("/sign-in");
    // Customers belong in the self-service portal, not the staff workspace.
    else if (status === "authenticated" && has(PERMISSIONS.PORTAL_CLAIM_READ)) {
      router.replace("/portal");
    }
  }, [status, has, router]);

  if (status !== "authenticated" || has(PERMISSIONS.PORTAL_CLAIM_READ)) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="text-muted-foreground h-6 w-6 animate-spin" />
      </div>
    );
  }

  return (
    <SidebarProvider>
      <RouteTitle />
      <AppSidebar />
      <SidebarInset className="min-w-0">
        {/* min-w-0 lets wide content (tables) scroll within its container instead of pushing the page */}
        <ImpersonationBanner />
        <header className="bg-background/95 supports-[backdrop-filter]:bg-background/60 sticky top-0 z-10 flex h-14 items-center gap-2 border-b px-4 backdrop-blur">
          <SidebarTrigger />
          <Separator orientation="vertical" className="mr-1 h-5!" />
          <div className="flex-1" />
          <NotificationsBell />
          <ThemeToggle />
          <UserMenu />
        </header>
        <main className="w-full min-w-0 flex-1 p-4 md:p-6">
          <div className="mx-auto w-full max-w-7xl space-y-6">{children}</div>
        </main>
        <StaffAssistant />
        <AppFooter />
      </SidebarInset>
    </SidebarProvider>
  );
}
