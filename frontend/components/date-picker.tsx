"use client";

import { useState } from "react";
import { CalendarIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import dayjs from "@/lib/dayjs";

/**
 * Date picker built from shadcn Popover + Calendar (react-day-picker). Value in/out is an ISO date
 * string (YYYY-MM-DD) to match the form fields and the backend LocalDate contract — never the native
 * <input type="date">.
 */
export function DatePicker({
  id,
  value,
  onChange,
  placeholder = "Pick a date",
  disabled,
}: {
  id?: string;
  value?: string | null;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const selected = value ? dayjs(value).toDate() : undefined;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          disabled={disabled}
          className={cn(
            "w-full justify-start text-left font-normal",
            !value && "text-muted-foreground",
          )}
        >
          <CalendarIcon className="mr-2 h-4 w-4" />
          {value ? dayjs(value).format("DD MMM YYYY") : placeholder}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          selected={selected}
          defaultMonth={selected}
          onSelect={(date) => {
            if (date) {
              onChange(dayjs(date).format("YYYY-MM-DD"));
              setOpen(false);
            }
          }}
          captionLayout="dropdown"
          startMonth={new Date(1950, 0)}
          endMonth={new Date(new Date().getFullYear() + 10, 11)}
          autoFocus
        />
      </PopoverContent>
    </Popover>
  );
}
