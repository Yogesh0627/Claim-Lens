"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { NAV_SECTIONS } from "@/lib/navigation";

// Titles for routes that aren't in the staff nav (platform console + customer portal).
const EXTRA: Record<string, string> = {
  "/platform": "Platform",
  "/platform/tenants": "Tenants",
  "/portal": "My claims",
  "/portal/policies": "My policies",
  "/portal/claims/new": "File a claim",
  "/portal/claims": "Claim",
};

function titleFor(pathname: string): string | null {
  // Longest matching nav item wins (so /claims/123 → "Claims").
  let best: { href: string; title: string } | null = null;
  for (const section of NAV_SECTIONS) {
    for (const item of section.items) {
      if (pathname === item.href || pathname.startsWith(item.href + "/")) {
        if (!best || item.href.length > best.href.length) {
          best = { href: item.href, title: item.title };
        }
      }
    }
  }
  if (best) return best.title;
  for (const [href, title] of Object.entries(EXTRA)) {
    if (pathname === href || pathname.startsWith(href + "/")) return title;
  }
  return null;
}

/**
 * Sets the browser tab title per route for the authenticated areas, which are client components (so
 * they can't export server `metadata`). These pages are behind auth (not crawled), so a client-side
 * document.title is the right tool — SEO metadata is reserved for the public pages.
 */
export function RouteTitle() {
  const pathname = usePathname();
  useEffect(() => {
    const t = titleFor(pathname);
    document.title = t ? `${t} · ClaimLens` : "ClaimLens";
  }, [pathname]);
  return null;
}
