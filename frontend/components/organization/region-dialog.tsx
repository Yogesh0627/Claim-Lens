"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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
import { REGION_STATUSES, optionLabel } from "@/lib/enums";
import type { RegionResponse } from "@/lib/types";

interface Form {
  code: string;
  name: string;
  status: string;
  description: string;
}

export function RegionDialog({
  open,
  onOpenChange,
  region,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  region?: RegionResponse | null;
  onSaved: () => void;
}) {
  const isEdit = !!region;
  const { register, handleSubmit, reset, watch, setValue } = useForm<Form>();
  const status = watch("status");

  useEffect(() => {
    reset({
      code: region?.code ?? "",
      name: region?.name ?? "",
      status: region?.status ?? "ACTIVE",
      description: region?.description ?? "",
    });
  }, [region, reset, open]);

  const save = useMutation(
    (v: Form) =>
      isEdit
        ? organizationService.updateRegion(region!.id, {
            name: v.name,
            status: v.status,
            description: v.description || null,
          })
        : organizationService.createRegion({
            code: v.code,
            name: v.name,
            status: v.status,
            description: v.description || null,
          }),
    {
      successMessage: isEdit ? "Region updated" : "Region created",
      onSuccess: () => {
        onOpenChange(false);
        onSaved();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit region" : "New region"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          {!isEdit ? (
            <div className="grid gap-2">
              <Label>Code *</Label>
              <Input {...register("code", { required: true })} />
            </div>
          ) : null}
          <div className="grid gap-2">
            <Label>Name *</Label>
            <Input {...register("name", { required: true })} />
          </div>
          <div className="grid gap-2">
            <Label>Status</Label>
            <Select value={status} onValueChange={(v) => setValue("status", v)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {REGION_STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {optionLabel(s)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid gap-2">
            <Label>Description</Label>
            <Textarea rows={2} {...register("description")} />
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
