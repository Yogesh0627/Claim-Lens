import { cn } from "@/lib/utils";
import { statusMeta, type Intent, type StatusMeta } from "@/lib/enums";

const INTENT_CLASSES: Record<Intent, string> = {
  neutral: "bg-muted text-muted-foreground",
  info: "bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300",
  progress: "bg-violet-100 text-violet-700 dark:bg-violet-950 dark:text-violet-300",
  success: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300",
  warning: "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300",
  danger: "bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300",
};

interface StatusBadgeProps {
  value?: string | null;
  map?: Record<string, StatusMeta>;
  className?: string;
}

/** A small pill whose colour is driven by the status "intent" (see lib/enums). */
export function StatusBadge({ value, map = {}, className }: StatusBadgeProps) {
  const meta = statusMeta(map, value);
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap",
        INTENT_CLASSES[meta.intent],
        className,
      )}
    >
      {meta.label}
    </span>
  );
}
