"use client";

import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

/** An icon-only button with a shadcn tooltip label (never the native HTML `title` attribute). */
export function IconButton({
  label,
  children,
  onClick,
  variant = "ghost",
  disabled,
}: {
  label: string;
  children: React.ReactNode;
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void;
  variant?: "ghost" | "outline" | "secondary";
  disabled?: boolean;
}) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          variant={variant}
          size="icon"
          aria-label={label}
          onClick={onClick}
          disabled={disabled}
        >
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  );
}
