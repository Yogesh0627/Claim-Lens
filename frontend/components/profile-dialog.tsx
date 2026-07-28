"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { LoadingRows, ErrorState } from "@/components/data-state";
import { FieldError } from "@/components/field-error";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { profileService } from "@/services/profileService";
import { optionLabel } from "@/lib/enums";

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4 py-1.5 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium">{value || "—"}</span>
    </div>
  );
}

export function ProfileDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const profile = useAsync(() => profileService.me(), [open], { enabled: open });

  const details = useForm<{ firstName: string; lastName: string; phone: string }>();
  const pwForm = useForm<{ currentPassword: string; newPassword: string }>();

  useEffect(() => {
    if (profile.data) {
      details.reset({
        firstName: profile.data.firstName ?? "",
        lastName: profile.data.lastName ?? "",
        phone: profile.data.phone ?? "",
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profile.data]);

  const save = useMutation(
    (v: { firstName: string; lastName: string; phone: string }) =>
      profileService.update({
        firstName: v.firstName,
        lastName: v.lastName || null,
        phone: v.phone || null,
      }),
    { successMessage: "Profile updated", onSuccess: () => profile.refetch() },
  );

  const changePw = useMutation(
    (v: { currentPassword: string; newPassword: string }) => profileService.changePassword(v),
    {
      successMessage: "Password changed",
      onSuccess: () => pwForm.reset({ currentPassword: "", newPassword: "" }),
    },
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>My profile</DialogTitle>
        </DialogHeader>

        {profile.loading ? (
          <LoadingRows />
        ) : profile.error || !profile.data ? (
          <ErrorState message={profile.error ?? "Could not load profile"} />
        ) : (
          <Tabs defaultValue="details">
            <TabsList>
              <TabsTrigger value="details">Details</TabsTrigger>
              <TabsTrigger value="password">Password</TabsTrigger>
            </TabsList>

            <TabsContent value="details" className="space-y-4">
              <div className="bg-muted/40 divide-y rounded-md px-3">
                <Row label="Email" value={profile.data.email} />
                <Row
                  label="Role"
                  value={
                    profile.data.roleName ??
                    (profile.data.roleCode ? optionLabel(profile.data.roleCode) : "—")
                  }
                />
                {profile.data.customerNumber ? (
                  <Row label="Customer number" value={profile.data.customerNumber} />
                ) : (
                  <>
                    {/* Staff org placement — always shown, so the fields are visible even when unset. */}
                    <Row label="Employee code" value={profile.data.employeeCode} />
                    <Row label="Department" value={profile.data.departmentName} />
                    <Row label="Designation" value={profile.data.designationName} />
                    <Row label="Region" value={profile.data.regionName} />
                    <Row
                      label="Home branch"
                      value={
                        profile.data.homeBranchName
                          ? profile.data.homeBranchLocation
                            ? `${profile.data.homeBranchName} (${profile.data.homeBranchLocation})`
                            : profile.data.homeBranchName
                          : null
                      }
                    />
                    <Row
                      label="Branches"
                      value={
                        profile.data.branchNames.length ? profile.data.branchNames.join(", ") : null
                      }
                    />
                  </>
                )}
              </div>

              <form
                onSubmit={details.handleSubmit((v) => save.run(v))}
                className="grid gap-3 sm:grid-cols-2"
              >
                <div className="grid gap-2">
                  <Label>First name *</Label>
                  <Input {...details.register("firstName", { required: true })} />
                  <FieldError name="firstName" errors={save.fieldErrors} />
                </div>
                <div className="grid gap-2">
                  <Label>Last name</Label>
                  <Input {...details.register("lastName")} />
                </div>
                <div className="grid gap-2 sm:col-span-2">
                  <Label>Phone</Label>
                  <Input {...details.register("phone")} />
                  <FieldError name="phone" errors={save.fieldErrors} />
                </div>
                <div className="sm:col-span-2">
                  <Button type="submit" disabled={save.loading}>
                    {save.loading ? "Saving…" : "Save changes"}
                  </Button>
                </div>
              </form>
            </TabsContent>

            <TabsContent value="password">
              <form
                onSubmit={pwForm.handleSubmit((v) => changePw.run(v))}
                className="grid gap-3"
              >
                <div className="grid gap-2">
                  <Label>Current password *</Label>
                  <Input
                    type="password"
                    {...pwForm.register("currentPassword", { required: true })}
                  />
                  <FieldError name="currentPassword" errors={changePw.fieldErrors} />
                </div>
                <div className="grid gap-2">
                  <Label>New password *</Label>
                  <Input
                    type="password"
                    {...pwForm.register("newPassword", { required: true, minLength: 6 })}
                  />
                  <FieldError name="newPassword" errors={changePw.fieldErrors} />
                  <p className="text-muted-foreground text-xs">At least 6 characters.</p>
                </div>
                <div>
                  <Button type="submit" disabled={changePw.loading}>
                    {changePw.loading ? "Changing…" : "Change password"}
                  </Button>
                </div>
              </form>
            </TabsContent>
          </Tabs>
        )}
      </DialogContent>
    </Dialog>
  );
}
