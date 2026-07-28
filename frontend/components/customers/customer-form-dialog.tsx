"use client";

import { useEffect, useState } from "react";
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
import { useMutation } from "@/hooks/useMutation";
import { customerService } from "@/services/customerService";
import { CUSTOMER_STATUSES, optionLabel } from "@/lib/enums";
import { toIsoDate } from "@/lib/dayjs";
import type { CustomerResponse } from "@/lib/types";

interface CustomerForm {
  customerNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  nationalId: string;
  city: string;
  state: string;
  status: string;
}

export function CustomerFormDialog({
  open,
  onOpenChange,
  customer,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  customer?: CustomerResponse | null;
  onSaved: () => void;
}) {
  const isEdit = !!customer;
  const { register, handleSubmit, reset, watch, setValue, control } = useForm<CustomerForm>();
  const status = watch("status");

  useEffect(() => {
    reset({
      customerNumber: customer?.customerNumber ?? "",
      firstName: customer?.firstName ?? "",
      lastName: customer?.lastName ?? "",
      email: customer?.email ?? "",
      phone: customer?.phone ?? "",
      dateOfBirth: customer?.dateOfBirth ?? "",
      nationalId: customer?.nationalId ?? "",
      city: customer?.city ?? "",
      state: customer?.state ?? "",
      status: customer?.status ?? "ACTIVE",
    });
  }, [customer, reset, open]);

  const save = useMutation(
    async (values: CustomerForm) => {
      const common = {
        firstName: values.firstName,
        lastName: values.lastName || null,
        email: values.email || null,
        phone: values.phone || null,
        dateOfBirth: toIsoDate(values.dateOfBirth) ?? null,
        nationalId: values.nationalId || null,
        city: values.city || null,
        state: values.state || null,
      };
      if (isEdit) {
        return customerService.update(customer!.id, { ...common, status: values.status });
      }
      return customerService.create({ ...common, customerNumber: values.customerNumber });
    },
    {
      successMessage: isEdit ? "Customer updated" : "Customer created",
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
          <DialogTitle>{isEdit ? "Edit customer" : "New customer"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          {!isEdit ? (
            <Field label="Customer number" required error={save.fieldErrors?.customerNumber}>
              <Input {...register("customerNumber", { required: true })} />
            </Field>
          ) : null}
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="First name" required error={save.fieldErrors?.firstName}>
              <Input {...register("firstName", { required: true })} />
            </Field>
            <Field label="Last name" error={save.fieldErrors?.lastName}>
              <Input {...register("lastName")} />
            </Field>
            <Field label="Email" error={save.fieldErrors?.email}>
              <Input type="email" {...register("email")} />
            </Field>
            <Field label="Phone" error={save.fieldErrors?.phone}>
              <Input {...register("phone")} />
            </Field>
            <Field label="Date of birth" error={save.fieldErrors?.dateOfBirth}>
              <Controller
                control={control}
                name="dateOfBirth"
                render={({ field }) => <DatePicker value={field.value} onChange={field.onChange} />}
              />
            </Field>
            <Field label="National ID" error={save.fieldErrors?.nationalId}>
              <Input {...register("nationalId")} />
            </Field>
            <Field label="City" error={save.fieldErrors?.city}>
              <Input {...register("city")} />
            </Field>
            <Field label="State" error={save.fieldErrors?.state}>
              <Input {...register("state")} />
            </Field>
          </div>
          {isEdit ? (
            <Field label="Status">
              <Select value={status} onValueChange={(v) => setValue("status", v)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CUSTOMER_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {optionLabel(s)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
          ) : null}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={save.loading}>
              {save.loading ? "Saving…" : "Save"}
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
  error,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid gap-2">
      <Label>
        {label}
        {required ? <span className="text-destructive"> *</span> : null}
      </Label>
      {children}
      {error ? <p className="text-destructive text-xs">{error}</p> : null}
    </div>
  );
}

/** Local hook wrapper so pages can open create/edit without wiring state each time. */
export function useCustomerDialog() {
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<CustomerResponse | null>(null);
  return {
    open,
    editing,
    openCreate: () => {
      setEditing(null);
      setOpen(true);
    },
    openEdit: (c: CustomerResponse) => {
      setEditing(c);
      setOpen(true);
    },
    setOpen,
  };
}
