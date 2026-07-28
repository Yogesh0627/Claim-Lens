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
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useMutation } from "@/hooks/useMutation";
import { organizationService } from "@/services/organizationService";
import { BRANCH_STATUSES, optionLabel } from "@/lib/enums";
import type { BranchResponse } from "@/lib/types";

interface Form {
  code: string;
  name: string;
  status: string;
  email: string;
  phone: string;
  city: string;
  state: string;
}

export function BranchDialog({
  regionId,
  open,
  onOpenChange,
  branch,
  onSaved,
}: {
  regionId: number;
  open: boolean;
  onOpenChange: (v: boolean) => void;
  branch?: BranchResponse | null;
  onSaved: () => void;
}) {
  const isEdit = !!branch;
  const { register, handleSubmit, reset, watch, setValue } = useForm<Form>();
  const status = watch("status");

  useEffect(() => {
    reset({
      code: branch?.code ?? "",
      name: branch?.name ?? "",
      status: branch?.status ?? "ACTIVE",
      email: branch?.email ?? "",
      phone: branch?.phone ?? "",
      city: branch?.city ?? "",
      state: branch?.state ?? "",
    });
  }, [branch, reset, open]);

  const save = useMutation(
    (v: Form) => {
      const common = {
        name: v.name,
        status: v.status,
        email: v.email || null,
        phone: v.phone || null,
        city: v.city || null,
        state: v.state || null,
      };
      return isEdit
        ? organizationService.updateBranch(regionId, branch!.id, common)
        : organizationService.createBranch(regionId, { ...common, code: v.code });
    },
    {
      successMessage: isEdit ? "Branch updated" : "Branch created",
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
          <DialogTitle>{isEdit ? "Edit branch" : "New branch"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            {!isEdit ? (
              <div className="grid gap-2">
                <Label>Code *</Label>
                <Input {...register("code", { required: true })} />
                <FieldError name="code" errors={save.fieldErrors} />
              </div>
            ) : null}
            <div className="grid gap-2">
              <Label>Name *</Label>
              <Input {...register("name", { required: true })} />
              <FieldError name="name" errors={save.fieldErrors} />
            </div>
            <div className="grid gap-2">
              <Label>Status</Label>
              <Select value={status} onValueChange={(v) => setValue("status", v)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {BRANCH_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {optionLabel(s)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label>Email</Label>
              <Input type="email" {...register("email")} />
              <FieldError name="email" errors={save.fieldErrors} />
            </div>
            <div className="grid gap-2">
              <Label>Phone</Label>
              <Input {...register("phone")} />
              <FieldError name="phone" errors={save.fieldErrors} />
            </div>
            <div className="grid gap-2">
              <Label>City</Label>
              <Input {...register("city")} />
            </div>
            <div className="grid gap-2">
              <Label>State</Label>
              <Input {...register("state")} />
            </div>
          </div>
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
