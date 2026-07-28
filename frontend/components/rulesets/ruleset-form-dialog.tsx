"use client";

import { useForm, useFieldArray } from "react-hook-form";
import { Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { rulesetService } from "@/services/rulesetService";

interface RulesetForm {
  name: string;
  claimTypeCode: string;
  mediumThreshold: number;
  highThreshold: number;
  rules: { code: string; description: string; weight: number; enabled: boolean }[];
}

const DEFAULT_RULES = [
  {
    code: "AMOUNT_OVER_SUM_INSURED",
    description: "Claim exceeds sum insured",
    weight: 30,
    enabled: true,
  },
  { code: "EARLY_CLAIM", description: "Claim soon after policy start", weight: 20, enabled: true },
];

export function RulesetFormDialog({
  open,
  onOpenChange,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSaved: () => void;
}) {
  const { register, handleSubmit, control, reset, watch, setValue } = useForm<RulesetForm>({
    defaultValues: {
      claimTypeCode: "MOTOR",
      mediumThreshold: 25,
      highThreshold: 50,
      rules: DEFAULT_RULES,
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "rules" });
  const rules = watch("rules");

  // The implemented rules the engine actually understands. Picking from these (instead of free text)
  // prevents adding a code with no evaluator, which the engine would silently ignore.
  const { data: catalog } = useAsync(() => rulesetService.ruleCatalog(), []);

  // When a rule code is chosen, auto-fill its description + suggested weight from the catalog.
  const chooseRule = (index: number, code: string) => {
    setValue(`rules.${index}.code`, code);
    const entry = catalog?.find((c) => c.code === code);
    if (entry) {
      setValue(`rules.${index}.description`, entry.description);
      setValue(`rules.${index}.weight`, entry.defaultWeight);
    }
  };

  const save = useMutation(
    (v: RulesetForm) =>
      rulesetService.create({
        claimTypeCode: v.claimTypeCode,
        name: v.name,
        mediumThreshold: Number(v.mediumThreshold),
        highThreshold: Number(v.highThreshold),
        rules: v.rules.map((r) => ({
          code: r.code,
          description: r.description || null,
          weight: Number(r.weight),
          enabled: r.enabled,
        })),
      }),
    {
      successMessage: "Ruleset created (draft)",
      onSuccess: () => {
        reset({
          claimTypeCode: "MOTOR",
          mediumThreshold: 25,
          highThreshold: 50,
          rules: DEFAULT_RULES,
        });
        onOpenChange(false);
        onSaved();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>New fraud ruleset</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label>Name *</Label>
              <Input {...register("name", { required: true })} />
            </div>
            <div className="grid gap-2">
              <Label>Claim type code *</Label>
              <Input {...register("claimTypeCode", { required: true })} />
            </div>
            <div className="grid gap-2">
              <Label>Medium threshold</Label>
              <Input type="number" {...register("mediumThreshold")} />
            </div>
            <div className="grid gap-2">
              <Label>High threshold</Label>
              <Input type="number" {...register("highThreshold")} />
            </div>
          </div>

          <div className="flex items-center justify-between">
            <Label>Rules</Label>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => append({ code: "", description: "", weight: 10, enabled: true })}
            >
              <Plus className="mr-1 h-4 w-4" /> Add rule
            </Button>
          </div>

          <div className="space-y-3">
            {fields.map((field, index) => (
              <div
                key={field.id}
                className="grid grid-cols-2 items-end gap-2 rounded-md border p-2 sm:grid-cols-[1fr_1fr_80px_auto_auto] sm:border-0 sm:p-0"
              >
                <div className="col-span-2 grid gap-1 sm:col-span-1">
                  <Label className="text-xs">Rule</Label>
                  {/* Hidden field keeps the code registered for validation; the Select drives it. */}
                  <input type="hidden" {...register(`rules.${index}.code` as const, { required: true })} />
                  <Select
                    value={rules?.[index]?.code || undefined}
                    onValueChange={(code) => chooseRule(index, code)}
                  >
                    <SelectTrigger className="cursor-pointer">
                      <SelectValue placeholder="Choose a rule" />
                    </SelectTrigger>
                    <SelectContent>
                      {(catalog ?? []).map((c) => (
                        <SelectItem key={c.code} value={c.code}>
                          {c.code}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="col-span-2 grid gap-1 sm:col-span-1">
                  <Label className="text-xs">Description</Label>
                  <Input
                    readOnly
                    className="text-muted-foreground"
                    {...register(`rules.${index}.description` as const)}
                  />
                </div>
                <div className="grid gap-1">
                  <Label className="text-xs">Weight</Label>
                  <Input type="number" {...register(`rules.${index}.weight` as const)} />
                </div>
                <div className="flex h-9 items-center gap-1">
                  <Checkbox
                    checked={rules?.[index]?.enabled}
                    onCheckedChange={(c) => setValue(`rules.${index}.enabled`, c === true)}
                  />
                  <span className="text-muted-foreground text-xs">On</span>
                </div>
                <div className="flex justify-end">
                  <Button type="button" variant="ghost" size="icon" onClick={() => remove(index)}>
                    <Trash2 className="text-destructive h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={save.loading}>
              {save.loading ? "Saving…" : "Create ruleset"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
