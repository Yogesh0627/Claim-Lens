"use client";

import { Controller, useForm } from "react-hook-form";
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
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { DatePicker } from "@/components/date-picker";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { customerService } from "@/services/customerService";
import { productService } from "@/services/productService";
import { policyService } from "@/services/policyService";
import type { CreatePolicyRequest } from "@/lib/types";

interface PolicyForm {
  policyNumber: string;
  customerId: string;
  insuranceProductId: string;
  effectiveFrom: string;
  effectiveTo: string;
  sumInsured: string;
  deductible: string;
  premiumAmount: string;
  currency: string;
  registrationNumber: string;
  make: string;
  model: string;
  variant: string;
  manufactureYear: string;
  chassisNumber: string;
  engineNumber: string;
}

export function PolicyFormDialog({
  open,
  onOpenChange,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSaved: () => void;
}) {
  const { register, handleSubmit, watch, setValue, reset, control } = useForm<PolicyForm>({
    defaultValues: { currency: "INR" },
  });
  const { data: customers } = useAsync(() => customerService.list(), []);
  const { data: products } = useAsync(() => productService.list(), []);
  const customerId = watch("customerId");
  const productId = watch("insuranceProductId");

  const save = useMutation(
    (values: PolicyForm) => {
      const body: CreatePolicyRequest = {
        policyNumber: values.policyNumber,
        customerId: Number(values.customerId),
        insuranceProductId: Number(values.insuranceProductId),
        effectiveFrom: values.effectiveFrom,
        effectiveTo: values.effectiveTo,
        sumInsured: Number(values.sumInsured),
        deductible: values.deductible ? Number(values.deductible) : null,
        premiumAmount: values.premiumAmount ? Number(values.premiumAmount) : null,
        currency: values.currency || "INR",
        vehicle: {
          registrationNumber: values.registrationNumber,
          make: values.make,
          model: values.model,
          variant: values.variant || null,
          manufactureYear: values.manufactureYear ? Number(values.manufactureYear) : null,
          chassisNumber: values.chassisNumber || null,
          engineNumber: values.engineNumber || null,
        },
      };
      return policyService.create(body);
    },
    {
      successMessage: "Policy created",
      onSuccess: () => {
        reset({ currency: "INR" });
        onOpenChange(false);
        onSaved();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>New policy</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Policy number" required>
              <Input {...register("policyNumber", { required: true })} />
            </Field>
            <Field label="Customer" required>
              <Select value={customerId} onValueChange={(v) => setValue("customerId", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select customer" />
                </SelectTrigger>
                <SelectContent>
                  {(customers ?? []).map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.firstName} {c.lastName ?? ""} · {c.customerNumber}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
            <Field label="Product" required>
              <Select value={productId} onValueChange={(v) => setValue("insuranceProductId", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select product" />
                </SelectTrigger>
                <SelectContent>
                  {(products ?? []).map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
            <Field label="Currency">
              <Input {...register("currency")} />
            </Field>
            <Field label="Effective from" required>
              <Controller
                control={control}
                name="effectiveFrom"
                rules={{ required: true }}
                render={({ field }) => <DatePicker value={field.value} onChange={field.onChange} />}
              />
            </Field>
            <Field label="Effective to" required>
              <Controller
                control={control}
                name="effectiveTo"
                rules={{ required: true }}
                render={({ field }) => <DatePicker value={field.value} onChange={field.onChange} />}
              />
            </Field>
            <Field label="Sum insured" required>
              <Input type="number" {...register("sumInsured", { required: true })} />
            </Field>
            <Field label="Deductible">
              <Input type="number" {...register("deductible")} />
            </Field>
            <Field label="Premium amount">
              <Input type="number" {...register("premiumAmount")} />
            </Field>
          </div>

          <p className="text-muted-foreground text-sm font-medium">Insured vehicle</p>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Registration" required>
              <Input
                placeholder="MH 12 AB 1234"
                {...register("registrationNumber", { required: true })}
              />
            </Field>
            <Field label="Make" required>
              <Input {...register("make", { required: true })} />
            </Field>
            <Field label="Model" required>
              <Input {...register("model", { required: true })} />
            </Field>
            <Field label="Variant">
              <Input {...register("variant")} />
            </Field>
            <Field label="Manufacture year">
              <Input type="number" {...register("manufactureYear")} />
            </Field>
            <Field label="Chassis number">
              <Input {...register("chassisNumber")} />
            </Field>
            <Field label="Engine number">
              <Input {...register("engineNumber")} />
            </Field>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={save.loading}>
              {save.loading ? "Saving…" : "Create policy"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function Field({
  label,
  required,
  children,
}: {
  label: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div className="grid gap-2">
      <Label>
        {label}
        {required ? <span className="text-destructive"> *</span> : null}
      </Label>
      {children}
    </div>
  );
}
