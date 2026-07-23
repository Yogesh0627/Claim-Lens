"use client";

import Link from "next/link";
import {
  ArrowRight,
  BarChart3,
  Building2,
  FileScan,
  History,
  LogIn,
  ScanText,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ThemeToggle } from "@/components/theme-toggle";
import { AppFooter } from "@/components/app-footer";
import { useAuth } from "@/hooks/useAuth";
import { homePathFor } from "@/lib/navigation";
import { initials } from "@/lib/format";

const FEATURES = [
  {
    icon: Building2,
    title: "Multi-tenant by design",
    body: "Every insurer is a fully isolated tenant. Enforced in the database, not by convention — two tenants can never see each other's data.",
  },
  {
    icon: ScanText,
    title: "OCR document intake",
    body: "RC books, FIRs, repair estimates and invoices are read automatically, with the key fields extracted and surfaced to investigators.",
  },
  {
    icon: FileScan,
    title: "Image fraud signals",
    body: "Perceptual-hash duplicate detection, ORB similarity and three-state EXIF checks catch reused and tampered damage photos.",
  },
  {
    icon: ShieldAlert,
    title: "Explainable fraud scoring",
    body: "A config-driven rules engine scores every claim and shows exactly why — because an automated decision must be auditable.",
  },
  {
    icon: Sparkles,
    title: "AI policy intelligence",
    body: "Ask coverage questions in plain language and get answers grounded in the policy wording — with citations back to the clauses.",
  },
  {
    icon: History,
    title: "Auditable by default",
    body: "Every material action is recorded to an append-only log, and live dashboards track claims and fraud risk across the portfolio.",
  },
];

const ROLES = [
  { title: "Tenant Admin", body: "Configure the org, products, policies, rulesets and users." },
  { title: "Investigation Manager", body: "Assign work, oversee investigators and monitor risk." },
  { title: "Investigator", body: "Work assigned claims, log findings and decide approve/reject." },
  { title: "Customer Support", body: "Create and submit claims on behalf of policyholders." },
  { title: "Auditor / Analyst", body: "Read audit trails and analytics across the tenant." },
  { title: "Product Manager", body: "Author products, versions and their policy knowledge base." },
];

const STEPS = [
  "Submit a claim with documents",
  "OCR + image analysis run automatically",
  "Fraud engine scores and explains it",
  "The right investigator is auto-assigned",
  "They investigate and decide — fully audited",
];

const PARTNERS = [
  { name: "Konoha Insurance", mark: "🍥" },
  { name: "Survey Corps Mutual", mark: "⚔️" },
  { name: "Capsule Corp Assurance", mark: "🧪" },
  { name: "UA Hero Cover", mark: "🦸" },
  { name: "Amestris General", mark: "⚗️" },
  { name: "Straw Hat Marine", mark: "🏴‍☠️" },
];

const TESTIMONIALS = [
  {
    name: "Light Yagami",
    role: "VP, Claims Investigation · Kira Assurance",
    quote:
      "ClaimLens flags the fraud I'd have spent all night hunting. Everything according to plan — just faster.",
  },
  {
    name: "Senku Ishigami",
    role: "Chief Data Officer · Kingdom of Science Assurance",
    quote:
      "Ten billion percent the smartest claims stack we've deployed. The policy Q&A answers are dead accurate.",
  },
  {
    name: "Mikasa Ackerman",
    role: "Head of Claims · Survey Corps Mutual",
    quote: "It protects what matters. My team now resolves claims in a fraction of the time.",
  },
  {
    name: "Hange Zoë",
    role: "Lead Fraud Analyst · Titan Shield",
    quote:
      "The duplicate-photo detection is beautiful — I could study its signals for hours. Absolutely fascinating!",
  },
  {
    name: "Roy Mustang",
    role: "Regional Manager · Amestris General",
    quote: "Auto-assignment balances my investigators perfectly. One clean sweep, every time.",
  },
  {
    name: "Tanjiro Kamado",
    role: "Customer Support Lead · Breath Insurance",
    quote: "Even on the hardest claim, customers feel cared for. That's everything to us.",
  },
];

export default function LandingPage() {
  const { status, permissions } = useAuth();
  const appHref = status === "authenticated" ? homePathFor(permissions) : "/sign-in";

  return (
    <div className="flex min-h-screen flex-col">
      {/* Nav */}
      <header className="bg-background/80 sticky top-0 z-20 border-b backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between px-4">
          <Link href="/" className="flex items-center gap-2">
            <span className="bg-primary text-primary-foreground flex h-8 w-8 items-center justify-center rounded-lg">
              <ShieldCheck className="h-5 w-5" />
            </span>
            <span className="text-lg font-semibold">ClaimLens</span>
          </Link>
          <div className="flex items-center gap-2">
            <Button asChild variant="ghost" size="sm" className="hidden sm:inline-flex">
              <Link href="/roadmap">Roadmap</Link>
            </Button>
            <Button asChild variant="ghost" size="sm">
              <Link href="/blog">Blog</Link>
            </Button>
            <ThemeToggle />
            <Button asChild size="sm">
              <Link href={appHref}>
                <LogIn className="mr-1 h-4 w-4" />
                {status === "authenticated" ? "Go to app" : "Sign in"}
              </Link>
            </Button>
          </div>
        </div>
      </header>

      <main className="flex-1">
        {/* Hero */}
        <section className="relative overflow-hidden">
          <div className="from-primary/5 pointer-events-none absolute inset-0 bg-linear-to-b to-transparent" />
          <div className="relative mx-auto w-full max-w-6xl px-4 py-20 text-center sm:py-28">
            <span className="bg-primary/10 text-primary mb-5 inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium">
              <Sparkles className="h-3.5 w-3.5" /> Motor-insurance claims, automated end to end
            </span>
            <h1 className="mx-auto max-w-3xl text-4xl font-bold tracking-tight text-balance sm:text-5xl">
              Investigate faster. Decide smarter.
            </h1>
            <p className="text-muted-foreground mx-auto mt-5 max-w-2xl text-lg text-pretty">
              ClaimLens automates the busywork of motor-insurance claims — OCR, image fraud
              analysis, rule-based scoring and AI policy answers — so your investigators focus on
              the decision. Automation prepares it; a human makes the call.
            </p>
            <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
              <Button asChild size="lg">
                <Link href={appHref}>
                  Sign in to your portal <ArrowRight className="ml-1 h-4 w-4" />
                </Link>
              </Button>
              <Button asChild size="lg" variant="outline">
                <Link href="#features">Explore the platform</Link>
              </Button>
            </div>
            <p className="text-muted-foreground mt-4 text-sm">
              One secure portal for tenant admins, investigators, managers and support teams.
            </p>
          </div>
        </section>

        {/* Trusted partners */}
        <section className="bg-muted/30 border-y">
          <div className="mx-auto w-full max-w-6xl px-4 py-8">
            <p className="text-muted-foreground text-center text-xs font-medium tracking-wide uppercase">
              Trusted by claims teams everywhere
            </p>
            <div className="mt-5 flex flex-wrap items-center justify-center gap-x-8 gap-y-4">
              {PARTNERS.map((p) => (
                <span
                  key={p.name}
                  className="text-muted-foreground flex items-center gap-2 text-sm font-semibold"
                >
                  <span className="text-lg">{p.mark}</span>
                  {p.name}
                </span>
              ))}
            </div>
          </div>
        </section>

        {/* Features */}
        <section id="features" className="mx-auto w-full max-w-6xl px-4 py-20">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-bold tracking-tight">Everything a claims desk needs</h2>
            <p className="text-muted-foreground mt-3">
              From intake to decision, with the intelligence built in.
            </p>
          </div>
          <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map((f) => (
              <Card key={f.title} className="transition-shadow hover:shadow-md">
                <CardContent className="pt-6">
                  <span className="bg-primary/10 text-primary mb-4 flex h-10 w-10 items-center justify-center rounded-lg">
                    <f.icon className="h-5 w-5" />
                  </span>
                  <h3 className="font-semibold">{f.title}</h3>
                  <p className="text-muted-foreground mt-2 text-sm">{f.body}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>

        {/* How it works */}
        <section className="bg-muted/30 border-y">
          <div className="mx-auto w-full max-w-6xl px-4 py-20">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-3xl font-bold tracking-tight">How a claim flows</h2>
              <p className="text-muted-foreground mt-3">
                A guided pipeline that keeps humans in control of the final call.
              </p>
            </div>
            <ol className="mx-auto mt-12 grid max-w-4xl gap-4 sm:grid-cols-5">
              {STEPS.map((step, i) => (
                <li
                  key={step}
                  className="flex flex-col items-center text-center sm:items-start sm:text-left"
                >
                  <span className="bg-primary text-primary-foreground flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold">
                    {i + 1}
                  </span>
                  <p className="mt-3 text-sm">{step}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* Roles / who signs in */}
        <section className="mx-auto w-full max-w-6xl px-4 py-20">
          <div className="mx-auto max-w-2xl text-center">
            <span className="bg-primary/10 text-primary mb-3 inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium">
              <Users className="h-3.5 w-3.5" /> One portal, every role
            </span>
            <h2 className="text-3xl font-bold tracking-tight">
              Tenant admins and their teams sign in here
            </h2>
            <p className="text-muted-foreground mt-3">
              Everyone signs in through the same secure portal and lands in a workspace tailored to
              their permissions.
            </p>
          </div>
          <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {ROLES.map((r) => (
              <div key={r.title} className="rounded-xl border p-5">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="text-primary h-4 w-4" />
                  <h3 className="font-semibold">{r.title}</h3>
                </div>
                <p className="text-muted-foreground mt-2 text-sm">{r.body}</p>
              </div>
            ))}
          </div>
          <div className="mt-10 flex justify-center">
            <Button asChild size="lg">
              <Link href={appHref}>
                <LogIn className="mr-1 h-4 w-4" /> Sign in
              </Link>
            </Button>
          </div>
        </section>

        {/* Testimonials */}
        <section className="bg-muted/30 border-t">
          <div className="mx-auto w-full max-w-6xl px-4 py-20">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-3xl font-bold tracking-tight">Loved by claims teams</h2>
              <p className="text-muted-foreground mt-3">
                What our (entirely fictional) customers say.
              </p>
            </div>
            <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {TESTIMONIALS.map((t) => (
                <Card key={t.name}>
                  <CardContent className="flex h-full flex-col pt-6">
                    <p className="text-sm">“{t.quote}”</p>
                    <div className="mt-4 flex items-center gap-3 pt-4">
                      <span className="bg-primary/10 text-primary flex h-9 w-9 items-center justify-center rounded-full text-xs font-semibold">
                        {initials(t.name.split(" ")[0], t.name.split(" ")[1])}
                      </span>
                      <div>
                        <p className="text-sm font-medium">{t.name}</p>
                        <p className="text-muted-foreground text-xs">{t.role}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </section>

        {/* CTA */}
        <section className="mx-auto w-full max-w-6xl px-4 py-20">
          <div className="bg-primary text-primary-foreground relative overflow-hidden rounded-2xl px-6 py-14 text-center">
            <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
              Ready to investigate faster?
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm opacity-90">
              Sign in to your ClaimLens workspace and see the full claim lifecycle in action.
            </p>
            <div className="mt-7 flex items-center justify-center gap-2">
              <Button asChild size="lg" variant="secondary">
                <Link href={appHref}>
                  <BarChart3 className="mr-1 h-4 w-4" /> Open the platform
                </Link>
              </Button>
            </div>
          </div>
        </section>
      </main>

      <AppFooter />
    </div>
  );
}
