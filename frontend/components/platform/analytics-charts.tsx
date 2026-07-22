"use client";

import { useMemo, useState } from "react";
import { cn } from "@/lib/utils";

/** Tiny trend line for a stat card — the monthly "new per month" series behind a headline total. */
export function Sparkline({ values, className }: { values: number[]; className?: string }) {
  const w = 96;
  const h = 28;
  const pad = 3;
  const max = Math.max(1, ...values);
  const pts = values.map((v, i) => {
    const x = pad + (i / Math.max(1, values.length - 1)) * (w - pad * 2);
    const y = h - pad - (v / max) * (h - pad * 2);
    return [x, y] as const;
  });
  const d = pts.map(([x, y], i) => `${i === 0 ? "M" : "L"}${x.toFixed(1)},${y.toFixed(1)}`).join(" ");
  const last = pts[pts.length - 1];
  return (
    <svg
      viewBox={`0 0 ${w} ${h}`}
      width={w}
      height={h}
      className={cn("text-foreground/55", className)}
      preserveAspectRatio="none"
      aria-hidden
    >
      <path d={d} fill="none" stroke="currentColor" strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round" />
      {last ? <circle cx={last[0]} cy={last[1]} r={2.2} fill="currentColor" /> : null}
    </svg>
  );
}

interface Point {
  label: string;
  value: number;
}

/**
 * A single-series area chart (change-over-time). One axis by design — two measures of different
 * scale go in two of these (small multiples), never a dual-axis chart. Crosshair + tooltip on hover.
 */
export function MiniAreaChart({ points, unit }: { points: Point[]; unit: string }) {
  const [hover, setHover] = useState<number | null>(null);
  const W = 320;
  const H = 132;
  const padX = 6;
  const padTop = 14;
  const padBottom = 22;
  const max = Math.max(1, ...points.map((p) => p.value));
  const baseline = H - padBottom;

  const geom = useMemo(() => {
    const x = (i: number) => padX + (i / Math.max(1, points.length - 1)) * (W - padX * 2);
    const y = (v: number) => baseline - (v / max) * (baseline - padTop);
    const coords = points.map((p, i) => [x(i), y(p.value)] as const);
    const line = coords.map(([cx, cy], i) => `${i === 0 ? "M" : "L"}${cx.toFixed(1)},${cy.toFixed(1)}`).join(" ");
    const area = coords.length
      ? `M${coords[0][0].toFixed(1)},${baseline} ${line.replace(/^M/, "L")} L${coords[coords.length - 1][0].toFixed(1)},${baseline} Z`
      : "";
    return { x, y, coords, line, area };
  }, [points, max, baseline]);

  const gradId = `area-${unit}`;

  return (
    <div className="relative">
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="text-foreground h-auto w-full"
        preserveAspectRatio="none"
        role="img"
        aria-label={`${unit} per month`}
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="currentColor" stopOpacity="0.16" />
            <stop offset="100%" stopColor="currentColor" stopOpacity="0.01" />
          </linearGradient>
        </defs>

        {/* recessive baseline */}
        <line x1={padX} y1={baseline} x2={W - padX} y2={baseline} className="text-border" stroke="currentColor" strokeWidth={1} />

        {geom.area ? <path d={geom.area} fill={`url(#${gradId})`} /> : null}
        {geom.line ? (
          <path d={geom.line} fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
        ) : null}

        {/* hover crosshair + point */}
        {hover !== null && geom.coords[hover] ? (
          <>
            <line
              x1={geom.coords[hover][0]}
              y1={padTop - 6}
              x2={geom.coords[hover][0]}
              y2={baseline}
              className="text-muted-foreground/40"
              stroke="currentColor"
              strokeWidth={1}
              strokeDasharray="3 3"
            />
            <circle cx={geom.coords[hover][0]} cy={geom.coords[hover][1]} r={3.5} fill="currentColor" />
            <circle
              cx={geom.coords[hover][0]}
              cy={geom.coords[hover][1]}
              r={6}
              className="text-background"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
            />
          </>
        ) : null}

        {/* invisible hit targets */}
        {points.map((p, i) => (
          <rect
            key={p.label}
            x={i === 0 ? 0 : (geom.x(i - 1) + geom.x(i)) / 2}
            y={0}
            width={
              i === 0
                ? (geom.x(0) + geom.x(1)) / 2 || W
                : i === points.length - 1
                  ? W - (geom.x(i - 1) + geom.x(i)) / 2
                  : (geom.x(i + 1) - geom.x(i - 1)) / 2
            }
            height={H}
            fill="transparent"
            onMouseEnter={() => setHover(i)}
            onMouseLeave={() => setHover(null)}
          />
        ))}
      </svg>

      {/* month labels */}
      <div className="text-muted-foreground mt-1 flex justify-between px-1 text-[10px]">
        {points.map((p) => (
          <span key={p.label}>{p.label.slice(5)}</span>
        ))}
      </div>

      {/* tooltip */}
      {hover !== null && points[hover] ? (
        <div
          className="bg-popover text-popover-foreground pointer-events-none absolute top-0 z-10 -translate-x-1/2 rounded-md border px-2 py-1 text-xs shadow-sm"
          style={{ left: `${(geom.coords[hover][0] / W) * 100}%` }}
        >
          <span className="font-medium">{points[hover].value}</span> {unit}
          <span className="text-muted-foreground"> · {points[hover].label}</span>
        </div>
      ) : null}
    </div>
  );
}

/** Fraud risk mix as a stacked bar. Reserved status colors (severe→calm) + a 2px gap between segments. */
export function RiskBar({ high, medium, low }: { high: number; medium: number; low: number }) {
  const total = high + medium + low;
  if (total === 0) {
    return <span className="text-muted-foreground text-xs">no scores</span>;
  }
  return (
    <div className="flex h-2.5 w-32 gap-[2px] overflow-hidden rounded-full" role="img" aria-label={`${high} high, ${medium} medium, ${low} low risk`}>
      {high > 0 ? <div className="bg-red-500" style={{ flexGrow: high }} /> : null}
      {medium > 0 ? <div className="bg-amber-500" style={{ flexGrow: medium }} /> : null}
      {low > 0 ? <div className="bg-emerald-500" style={{ flexGrow: low }} /> : null}
    </div>
  );
}
