"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FieldError } from "@/components/field-error";
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
import type { InsuranceCompanyResponse, UpdateTenantRequest } from "@/lib/types";

export function EditTenantDialog({
  tenant,
  open,
  onOpenChange,
  onSaved,
}: {
  tenant: InsuranceCompanyResponse | null;
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSaved: () => void;
}) {
  const { register, handleSubmit, reset, watch, setValue } = useForm<UpdateTenantRequest>();
  const plan = watch("subscriptionPlan");

  // Re-seed the form whenever a different tenant is opened for editing.
  useEffect(() => {
    if (tenant) {
      reset({
        name: tenant.name,
        subscriptionPlan: tenant.subscriptionPlan,
        currency: tenant.currency,
        timezone: tenant.timezone,
        contactEmail: tenant.contactEmail ?? "",
      });
    }
  }, [tenant, reset]);

  const save = useMutation(
    (body: UpdateTenantRequest) => platformService.update(tenant!.id, body),
    {
      successMessage: "Tenant updated",
      onSuccess: () => {
        onOpenChange(false);
        onSaved();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Edit tenant</DialogTitle>
          <DialogDescription>
            Update {tenant?.name}. Code and tenant key are fixed identity and can&apos;t be changed.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-2">
            <Label>Company name *</Label>
            <Input {...register("name", { required: true })} placeholder="Acme Insurance" />
            <FieldError name="name" errors={save.fieldErrors} />
          </div>
          {/* Code + tenant key shown read-only for context — they are immutable identity. */}
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label className="text-muted-foreground">Code</Label>
              <Input value={tenant?.code ?? ""} disabled />
            </div>
            <div className="grid gap-2">
              <Label className="text-muted-foreground">Tenant key</Label>
              <Input value={tenant?.tenantKey ?? ""} disabled />
            </div>
            <div className="grid gap-2">
              <Label>Plan</Label>
              <Select value={plan} onValueChange={(v) => setValue("subscriptionPlan", v)}>
                <SelectTrigger className="cursor-pointer">
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
              <FieldError name="currency" errors={save.fieldErrors} />
            </div>
            <div className="grid gap-2">
              <Label>Timezone *</Label>
              <Input {...register("timezone", { required: true })} />
              <FieldError name="timezone" errors={save.fieldErrors} />
            </div>
            <div className="grid gap-2">
              <Label>Contact email</Label>
              <Input type="email" {...register("contactEmail")} />
              <FieldError name="contactEmail" errors={save.fieldErrors} />
            </div>
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              className="cursor-pointer"
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button type="submit" className="cursor-pointer" disabled={save.loading}>
              {save.loading ? "Saving…" : "Save changes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
