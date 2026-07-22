"use client";

import { Controller, useForm } from "react-hook-form";
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
import { DatePicker } from "@/components/date-picker";
import { useMutation } from "@/hooks/useMutation";
import { productService } from "@/services/productService";
import { optionLabel } from "@/lib/enums";
import type { ProductResponse } from "@/lib/types";

const PRODUCT_STATUSES = ["ACTIVE", "INACTIVE", "RETIRED"] as const;

interface ProductForm {
  code: string;
  name: string;
  description: string;
  claimTypeCode: string;
}

export function ProductFormDialog({
  open,
  onOpenChange,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSaved: () => void;
}) {
  const { register, handleSubmit, reset } = useForm<ProductForm>({
    defaultValues: { claimTypeCode: "MOTOR" },
  });
  const save = useMutation(
    (v: ProductForm) =>
      productService.create({
        code: v.code,
        name: v.name,
        description: v.description || null,
        claimTypeCode: v.claimTypeCode,
      }),
    {
      successMessage: "Product created",
      onSuccess: () => {
        reset({ claimTypeCode: "MOTOR" });
        onOpenChange(false);
        onSaved();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New product</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-2">
            <Label>Code *</Label>
            <Input placeholder="PVT_CAR_PREMIUM" {...register("code", { required: true })} />
          </div>
          <div className="grid gap-2">
            <Label>Name *</Label>
            <Input {...register("name", { required: true })} />
          </div>
          <div className="grid gap-2">
            <Label>Claim type code *</Label>
            <Input {...register("claimTypeCode", { required: true })} />
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
              {save.loading ? "Saving…" : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

interface EditProductForm {
  name: string;
  description: string;
  status: string;
}

export function EditProductDialog({
  product,
  open,
  onOpenChange,
  onSaved,
}: {
  product: ProductResponse;
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSaved: () => void;
}) {
  const { register, handleSubmit, reset, control } = useForm<EditProductForm>({
    defaultValues: {
      name: product.name,
      description: product.description ?? "",
      status: product.status,
    },
  });
  const save = useMutation(
    (v: EditProductForm) =>
      productService.update(product.id, {
        name: v.name,
        description: v.description || null,
        status: v.status,
      }),
    {
      successMessage: "Product updated",
      onSuccess: () => {
        onOpenChange(false);
        onSaved();
      },
    },
  );

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (v)
          reset({
            name: product.name,
            description: product.description ?? "",
            status: product.status,
          });
        onOpenChange(v);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit product</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-2">
            <Label>Code</Label>
            <Input value={product.code} disabled />
          </div>
          <div className="grid gap-2">
            <Label>Name *</Label>
            <Input {...register("name", { required: true })} />
          </div>
          <div className="grid gap-2">
            <Label>Status *</Label>
            <Controller
              control={control}
              name="status"
              rules={{ required: true }}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {PRODUCT_STATUSES.map((s) => (
                      <SelectItem key={s} value={s}>
                        {optionLabel(s)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
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

interface VersionForm {
  effectiveFrom: string;
  effectiveTo: string;
  coverageSummary: string;
}

export function VersionFormDialog({
  productId,
  open,
  onOpenChange,
  onSaved,
}: {
  productId: number;
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSaved: () => void;
}) {
  const { register, handleSubmit, reset, control } = useForm<VersionForm>();
  const save = useMutation(
    (v: VersionForm) =>
      productService.createVersion(productId, {
        effectiveFrom: v.effectiveFrom,
        effectiveTo: v.effectiveTo || null,
        coverageSummary: v.coverageSummary || null,
      }),
    {
      successMessage: "Version created (draft)",
      onSuccess: () => {
        reset();
        onOpenChange(false);
        onSaved();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New product version</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label>Effective from *</Label>
              <Controller
                control={control}
                name="effectiveFrom"
                rules={{ required: true }}
                render={({ field }) => <DatePicker value={field.value} onChange={field.onChange} />}
              />
            </div>
            <div className="grid gap-2">
              <Label>Effective to</Label>
              <Controller
                control={control}
                name="effectiveTo"
                render={({ field }) => <DatePicker value={field.value} onChange={field.onChange} />}
              />
            </div>
          </div>
          <div className="grid gap-2">
            <Label>Coverage summary</Label>
            <Textarea rows={3} {...register("coverageSummary")} />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={save.loading}>
              {save.loading ? "Saving…" : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
