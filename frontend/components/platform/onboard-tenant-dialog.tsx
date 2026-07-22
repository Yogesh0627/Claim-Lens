"use client";

import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useMutation } from "@/hooks/useMutation";
import { platformService } from "@/services/platformService";
import { SUBSCRIPTION_PLANS, optionLabel } from "@/lib/enums";
import type { OnboardTenantRequest } from "@/lib/types";

export function OnboardTenantDialog({
  open,
  onOpenChange,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSaved: () => void;
}) {
  const { register, handleSubmit, reset, watch, setValue } = useForm<OnboardTenantRequest>({
    defaultValues: {
      subscriptionPlan: "BASIC",
      currency: "INR",
      timezone: "Asia/Kolkata",
    },
  });
  const plan = watch("subscriptionPlan");

  const save = useMutation((body: OnboardTenantRequest) => platformService.onboard(body), {
    successMessage: "Tenant onboarded",
    onSuccess: () => {
      reset({ subscriptionPlan: "BASIC", currency: "INR", timezone: "Asia/Kolkata" });
      onOpenChange(false);
      onSaved();
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Onboard a tenant</DialogTitle>
          <DialogDescription>
            Create a new insurance company. It starts in ONBOARDING status.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-2">
            <Label>Company name *</Label>
            <Input {...register("name", { required: true })} placeholder="Acme Insurance" />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label>Code *</Label>
              <Input {...register("code", { required: true })} placeholder="ACME" />
            </div>
            <div className="grid gap-2">
              <Label>Tenant key *</Label>
              <Input {...register("tenantKey", { required: true })} placeholder="acme" />
            </div>
            <div className="grid gap-2">
              <Label>Plan</Label>
              <Select value={plan} onValueChange={(v) => setValue("subscriptionPlan", v)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SUBSCRIPTION_PLANS.map((p) => (
                    <SelectItem key={p} value={p}>
                      {optionLabel(p)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label>Currency *</Label>
              <Input {...register("currency", { required: true })} />
            </div>
            <div className="grid gap-2">
              <Label>Timezone *</Label>
              <Input {...register("timezone", { required: true })} />
            </div>
            <div className="grid gap-2">
              <Label>Contact email</Label>
              <Input type="email" {...register("contactEmail")} />
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={save.loading}>
              {save.loading ? "Onboarding…" : "Onboard"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
