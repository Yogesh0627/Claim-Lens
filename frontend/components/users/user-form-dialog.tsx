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
import { customerService } from "@/services/customerService";
import { organizationService } from "@/services/organizationService";
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
  customerId: string;
  departmentId: string;
  designationId: string;
  regionId: string;
  homeBranchId: string;
}

/** A CUSTOMER login must point at a policyholder — the portal scopes every read by it. */
const CUSTOMER_ROLE = "CUSTOMER";

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

  const isCustomerRole = roleCode === CUSTOMER_ROLE;
  const customerId = watch("customerId");

  // Org placement — staff only. Loaded once the dialog opens.
  const { data: departments } = useAsync(
    () => (open ? organizationService.listDepartments() : Promise.resolve([])),
    [open],
  );
  const { data: designations } = useAsync(
    () => (open ? organizationService.listDesignations() : Promise.resolve([])),
    [open],
  );
  const { data: branches } = useAsync(
    () => (open ? organizationService.listAllBranches() : Promise.resolve([])),
    [open],
  );
  const { data: regions } = useAsync(
    () => (open ? organizationService.listRegions() : Promise.resolve([])),
    [open],
  );
  const departmentId = watch("departmentId");
  const designationId = watch("designationId");
  const regionId = watch("regionId");
  const homeBranchId = watch("homeBranchId");
  // Multi-branch selection lives outside react-hook-form (a set of branch ids the user works at).
  const [branchIds, setBranchIds] = useState<number[]>([]);
  const toggleBranch = (id: number) =>
    setBranchIds((cur) => (cur.includes(id) ? cur.filter((b) => b !== id) : [...cur, id]));
  // Only fetched when it's actually needed — creating a portal login.
  const { data: customers } = useAsync(
    () => (open && isCustomerRole && !isEdit ? customerService.options() : Promise.resolve([])),
    [open, isCustomerRole, isEdit],
  );

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
      customerId: "",
      departmentId: user?.departmentId ? String(user.departmentId) : "",
      designationId: user?.designationId ? String(user.designationId) : "",
      regionId: user?.regionId ? String(user.regionId) : "",
      homeBranchId: user?.homeBranchId ? String(user.homeBranchId) : "",
    });
    setBranchIds(user?.branches ? user.branches.map((b) => b.id) : []);
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
            departmentId: v.departmentId ? Number(v.departmentId) : null,
            designationId: v.designationId ? Number(v.designationId) : null,
            regionId: v.regionId ? Number(v.regionId) : null,
            homeBranchId: v.homeBranchId ? Number(v.homeBranchId) : null,
            branchIds,
          })
        : userService.create({
            email: v.email,
            firstName: v.firstName,
            lastName: v.lastName || null,
            employeeCode: v.employeeCode,
            phone: v.phone || null,
            roleCode: v.roleCode,
            password: v.password || null,
            // Only a CUSTOMER carries one; the backend rejects it on staff roles.
            customerId: v.roleCode === CUSTOMER_ROLE && v.customerId ? Number(v.customerId) : null,
            departmentId: v.departmentId ? Number(v.departmentId) : null,
            designationId: v.designationId ? Number(v.designationId) : null,
            regionId: v.regionId ? Number(v.regionId) : null,
            homeBranchId: v.homeBranchId ? Number(v.homeBranchId) : null,
            branchIds,
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
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit user" : "New user"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit((v) => save.run(v))} className="grid gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Email" required error={save.fieldErrors?.email}>
              <Input type="email" disabled={isEdit} {...register("email", { required: !isEdit })} />
            </Field>
            {/* A customer is a policyholder, not staff — no employee code. The backend auto-generates
                one for CUSTOMER logins, so this field is hidden and not required for that role. */}
            {!isCustomerRole ? (
              <Field label="Employee code" required error={save.fieldErrors?.employeeCode}>
                <Input
                  disabled={isEdit}
                  {...register("employeeCode", { required: !isEdit && !isCustomerRole })}
                />
              </Field>
            ) : null}
            <Field label="First name" required error={save.fieldErrors?.firstName}>
              <Input {...register("firstName", { required: true })} />
            </Field>
            <Field label="Last name" error={save.fieldErrors?.lastName}>
              <Input {...register("lastName")} />
            </Field>
            <Field label="Phone" error={save.fieldErrors?.phone}>
              <Input {...register("phone")} />
            </Field>
            <Field label="Role" required error={save.fieldErrors?.roleCode}>
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
            {/* Org placement — staff only (a policyholder has none). */}
            {!isCustomerRole ? (
              <>
                <Field label="Department">
                  <Select
                    value={departmentId}
                    onValueChange={(v) => setValue("departmentId", v)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select department" />
                    </SelectTrigger>
                    <SelectContent>
                      {(departments ?? []).map((d) => (
                        <SelectItem key={d.id} value={String(d.id)}>
                          {d.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field label="Designation">
                  <Select
                    value={designationId}
                    onValueChange={(v) => setValue("designationId", v)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select designation" />
                    </SelectTrigger>
                    <SelectContent>
                      {(designations ?? []).map((d) => (
                        <SelectItem key={d.id} value={String(d.id)}>
                          {d.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field label="Region">
                  <Select value={regionId} onValueChange={(v) => setValue("regionId", v)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select region" />
                    </SelectTrigger>
                    <SelectContent>
                      {(regions ?? []).map((r) => (
                        <SelectItem key={r.id} value={String(r.id)}>
                          {r.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field label="Home branch">
                  <Select value={homeBranchId} onValueChange={(v) => setValue("homeBranchId", v)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select branch" />
                    </SelectTrigger>
                    <SelectContent>
                      {(branches ?? []).map((b) => (
                        <SelectItem key={b.id} value={String(b.id)}>
                          {b.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                {/* Multi-branch — every branch the user works at (home is auto-included). */}
                <div className="grid gap-2 sm:col-span-2">
                  <Label>Works at (branches)</Label>
                  <div className="flex flex-wrap gap-x-4 gap-y-2 rounded-md border p-3">
                    {(branches ?? []).length === 0 ? (
                      <span className="text-muted-foreground text-sm">No branches yet.</span>
                    ) : (
                      (branches ?? []).map((b) => {
                        const checked =
                          branchIds.includes(b.id) || String(b.id) === homeBranchId;
                        return (
                          <label
                            key={b.id}
                            className="flex cursor-pointer items-center gap-2 text-sm"
                          >
                            <input
                              type="checkbox"
                              className="accent-primary h-4 w-4"
                              checked={checked}
                              disabled={String(b.id) === homeBranchId}
                              onChange={() => toggleBranch(b.id)}
                            />
                            {b.name}
                            {String(b.id) === homeBranchId ? (
                              <span className="text-muted-foreground text-xs">(home)</span>
                            ) : null}
                          </label>
                        );
                      })
                    )}
                  </div>
                </div>
              </>
            ) : null}
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
              <Field
                label="Initial password"
                error={save.fieldErrors?.password}
                hint="At least 6 characters. Leave blank to email an invite instead."
              >
                <Input type="password" placeholder="Leave blank to invite" {...register("password")} />
              </Field>
            )}
          </div>

          {/* A portal login is useless unless it points at a policyholder, so this is required
              whenever the role is CUSTOMER — the backend rejects the request otherwise. */}
          {!isEdit && isCustomerRole ? (
            <Field label="Policyholder" required error={save.fieldErrors?.customerId}>
              <Select value={customerId} onValueChange={(v) => setValue("customerId", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select the customer this login belongs to" />
                </SelectTrigger>
                <SelectContent>
                  {(customers ?? []).map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.firstName} {c.lastName ?? ""} · {c.customerNumber}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-muted-foreground text-xs">
                The customer portal only shows claims and policies belonging to this person.
              </p>
            </Field>
          ) : null}

          <p className="text-muted-foreground text-xs">
            {isEdit
              ? null
              : "Leave the password blank and we'll email them a link to set their own — nobody else ever knows it."}
          </p>
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
  hint,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid gap-2">
      <Label>
        {label}
        {required ? <span className="text-destructive"> *</span> : null}
      </Label>
      {children}
      {error ? (
        <p className="text-destructive text-xs">{error}</p>
      ) : hint ? (
        <p className="text-muted-foreground text-xs">{hint}</p>
      ) : null}
    </div>
  );
}
