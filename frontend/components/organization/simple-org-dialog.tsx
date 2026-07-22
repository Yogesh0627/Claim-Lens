"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useMutation } from "@/hooks/useMutation";

interface SimpleEntity {
  id: number;
  code: string;
  name: string;
  description: string | null;
}

interface Form {
  code: string;
  name: string;
  description: string;
}

/** Shared create/edit dialog for the code+name+description org entities (departments, designations). */
export function SimpleOrgDialog<T extends SimpleEntity>({
  title,
  open,
  onOpenChange,
  entity,
  onCreate,
  onUpdate,
  onSaved,
}: {
  title: string;
  open: boolean;
  onOpenChange: (v: boolean) => void;
  entity?: T | null;
  onCreate: (body: { code: string; name: string; description: string | null }) => Promise<unknown>;
  onUpdate: (id: number, body: { name: string; description: string | null }) => Promise<unknown>;
  onSaved: () => void;
}) {
  const isEdit = !!entity;
  const { register, handleSubmit, reset } = useForm<Form>();

  useEffect(() => {
    reset({
      code: entity?.code ?? "",
      name: entity?.name ?? "",
      description: entity?.description ?? "",
    });
  }, [entity, reset, open]);

  const save = useMutation(
    (v: Form) =>
      isEdit
        ? onUpdate(entity!.id, { name: v.name, description: v.description || null })
        : onCreate({ code: v.code, name: v.name, description: v.description || null }),
    {
      successMessage: isEdit ? "Updated" : "Created",
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
          <DialogTitle>
            {isEdit ? "Edit" : "New"} {title}
          </DialogTitle>
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

/** Small helper for pages to manage create/edit dialog state. */
export function useCrudDialog<T>() {
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<T | null>(null);
  return {
    open,
    editing,
    setOpen,
    openCreate: () => {
      setEditing(null);
      setOpen(true);
    },
    openEdit: (e: T) => {
      setEditing(e);
      setOpen(true);
    },
  };
}
