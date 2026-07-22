"use client";

import { useEffect, useState } from "react";
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
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { userService } from "@/services/userService";
import { roleService } from "@/services/miscService";
import { optionLabel } from "@/lib/enums";
import type { UserResponse } from "@/lib/types";

interface UserForm {
  email: string;
  firstName: string;
  lastName: string;
  employeeCode: string;
  phone: string;
  roleCode: string;
  password: string;
  status: string;
}

const STATUSES = ["ACTIVE", "SUSPENDED", "TERMINATED", "INVITED"] as const;

export function UserFormDialog({
  open,
  onOpenChange,
  user,
  onSaved,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  user?: UserResponse | null;
  onSaved: () => void;
}) {
  const isEdit = !!user;
  const { register, handleSubmit, reset, watch, setValue } = useForm<UserForm>();
  const roleCode = watch("roleCode");
  const status = watch("status");

  // Tenant admins can't assign the platform-admin role.
  const { data: roles } = useAsync(() => roleService.list(), []);
  const assignableRoles = (roles ?? []).filter((r) => r.code !== "PLATFORM_ADMIN");

  useEffect(() => {
    reset({
      email: user?.email ?? "",
      firstName: user?.firstName ?? "",
      lastName: user?.lastName ?? "",
      employeeCode: user?.employeeCode ?? "",
      phone: user?.phone ?? "",
      roleCode: user?.roleCode ?? "",
      password: "",
      status: user?.status ?? "ACTIVE",
    });
  }, [user, reset, open]);

  const save = useMutation(
    (v: UserForm) =>
      isEdit
        ? userService.update(user!.id, {
            firstName: v.firstName,
            lastName: v.lastName || null,
            phone: v.phone || null,
            roleCode: v.roleCode,
            status: v.status,
          })
        : userService.create({
            email: v.email,
            firstName: v.firstName,
            lastName: v.lastName || null,
            employeeCode: v.employeeCode,
            phone: v.phone || null,
            roleCode: v.roleCode,
            password: v.password || null,
          }),
    {
      successMessage: isEdit ? "User updated" : "User created",
      onSuccess: () => {
        onOpenChange(false);
        onSaved();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit user" : "New user"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Email" required>
              <Input type="email" disabled={isEdit} {...register("email", { required: !isEdit })} />
            </Field>
            <Field label="Employee code" required>
              <Input disabled={isEdit} {...register("employeeCode", { required: !isEdit })} />
            </Field>
            <Field label="First name" required>
              <Input {...register("firstName", { required: true })} />
            </Field>
            <Field label="Last name">
              <Input {...register("lastName")} />
            </Field>
            <Field label="Phone">
              <Input {...register("phone")} />
            </Field>
            <Field label="Role" required>
              <Select value={roleCode} onValueChange={(v) => setValue("roleCode", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select role" />
                </SelectTrigger>
                <SelectContent>
                  {assignableRoles.map((r) => (
                    <SelectItem key={r.code} value={r.code}>
                      {r.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
            {isEdit ? (
              <Field label="Status">
                <Select value={status} onValueChange={(v) => setValue("status", v)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {STATUSES.map((s) => (
                      <SelectItem key={s} value={s}>
                        {optionLabel(s)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>
            ) : (
              <Field label="Initial password">
                <Input type="password" placeholder="Leave blank to invite" {...register("password")} />
              </Field>
            )}
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
