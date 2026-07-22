import Link from "next/link";
import { ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { AppFooter } from "@/components/app-footer";

export default function RoadmapLayout({ children }: { children: React.ReactNode }) {
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
          <span className="text-muted-foreground hidden text-sm sm:inline">/ Roadmap</span>
          <div className="ml-auto flex items-center gap-2">
            <Button asChild variant="ghost" size="sm">
              <Link href="/blog">Blog</Link>
            </Button>
            <ThemeToggle />
            <Button asChild variant="outline" size="sm">
              <Link href="/login">Sign in</Link>
            </Button>
          </div>
        </div>
      </header>
      <main className="w-full flex-1">{children}</main>
      <AppFooter />
    </div>
  );
}
