import type { Metadata } from "next";
import Link from "next/link";
import { ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { AppFooter } from "@/components/app-footer";

export const metadata: Metadata = {
  title: "Sign in",
  description: "Sign in to your ClaimLens workspace to manage motor-insurance claims.",
  robots: { index: true, follow: true },
};

/**
 * Public chrome, matching the other public pages. Without it the sign-in page was a dead end —
 * anyone landing here (or bounced here by an expired session) had no way back to the site.
 */
export default function LoginLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="bg-background/80 sticky top-0 z-20 border-b backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-5xl items-center gap-3 px-4 sm:px-6">
          <Link href="/" className="flex items-center gap-2">
            <span className="bg-primary text-primary-foreground flex h-8 w-8 items-center justify-center rounded-lg">
              <ShieldCheck className="h-5 w-5" />
            </span>
            <span className="text-lg font-semibold">ClaimLens</span>
          </Link>
          <span className="text-muted-foreground hidden text-sm sm:inline">/ Sign in</span>
          <div className="ml-auto flex items-center gap-1 sm:gap-2">
            <Button asChild variant="ghost" size="sm">
              <Link href="/roadmap">Roadmap</Link>
            </Button>
            <Button asChild variant="ghost" size="sm">
              <Link href="/blog">Blog</Link>
            </Button>
            <ThemeToggle />
          </div>
        </div>
      </header>
      <main className="flex w-full flex-1 flex-col">{children}</main>
      <AppFooter />
    </div>
  );
}
