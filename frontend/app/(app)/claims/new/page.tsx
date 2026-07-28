"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent } from "@/components/ui/card";
import { DatePicker } from "@/components/date-picker";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { customerService } from "@/services/customerService";
import { policyService } from "@/services/policyService";
import { claimService } from "@/services/claimService";
import type { CreateClaimRequest } from "@/lib/types";

interface ClaimForm {
  customerId: string;
  insurancePolicyId: string;
  incidentDate: string;
  claimAmount: string;
  vehicleRegistrationNumber: string;
  description: string;
}

export default function NewClaimPage() {
  const router = useRouter();
  const { register, handleSubmit, watch, setValue, control, formState } = useForm<ClaimForm>({
    defaultValues: {
      customerId: "",
      insurancePolicyId: "",
      incidentDate: "",
      claimAmount: "",
      vehicleRegistrationNumber: "",
      description: "",
    },
  });

  const { data: customers } = useAsync(() => customerService.options(), []);
  const { data: policies, error: policiesError } = useAsync(() => policyService.options(), []);

  const customerId = watch("customerId");
  const policyId = watch("insurancePolicyId");

  const customerPolicies = useMemo(
    () => (policies ?? []).filter((p) => String(p.customerId) === customerId),
    [policies, customerId],
  );

  // When a policy is chosen, prefill the vehicle registration from the policy's vehicle.
  const [prefilledFor, setPrefilledFor] = useState<string>("");
  const selectedPolicy = (policies ?? []).find((p) => String(p.id) === policyId);
  if (selectedPolicy && prefilledFor !== policyId) {
    setPrefilledFor(policyId);
    setValue("vehicleRegistrationNumber", selectedPolicy.vehicle?.registrationNumber ?? "");
  }

  const create = useMutation(claimService.create, {
    successMessage: "Draft claim created",
    onSuccess: (claim) => router.replace(`/claims/${claim.id}`),
  });

  const onSubmit = (values: ClaimForm) => {
    const body: CreateClaimRequest = {
      customerId: Number(values.customerId),
      insurancePolicyId: Number(values.insurancePolicyId),
      incidentDate: values.incidentDate,
      claimAmount: values.claimAmount ? Number(values.claimAmount) : null,
      vehicleRegistrationNumber: values.vehicleRegistrationNumber || null,
      description: values.description || null,
    };
    return create.run(body);
  };

  return (
    <>
      <PageHeader
        title="New claim"
        description="Create a draft claim, then upload documents and submit."
      />
      <Card className="max-w-2xl">
        <CardContent className="pt-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div className="grid gap-2">
              <Label htmlFor="customerId">Customer</Label>
              <Select
                value={customerId}
                onValueChange={(v) => {
                  setValue("customerId", v);
                  setValue("insurancePolicyId", "");
                }}
              >
                <SelectTrigger id="customerId">
                  <SelectValue placeholder="Select a customer" />
                </SelectTrigger>
                <SelectContent>
                  {(customers ?? []).map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.firstName} {c.lastName ?? ""} · {c.customerNumber}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="insurancePolicyId">Policy</Label>
              {policiesError ? (
                <Input
                  id="insurancePolicyId"
                  type="number"
                  placeholder="Policy ID"
                  {...register("insurancePolicyId", { required: true })}
                />
              ) : (
                <Select
                  value={policyId}
                  onValueChange={(v) => setValue("insurancePolicyId", v)}
                  disabled={!customerId}
                >
                  <SelectTrigger id="insurancePolicyId">
                    <SelectValue
                      placeholder={customerId ? "Select a policy" : "Select a customer first"}
                    />
                  </SelectTrigger>
                  <SelectContent>
                    {customerPolicies.map((p) => (
                      <SelectItem key={p.id} value={String(p.id)}>
                        {p.policyNumber} · {p.vehicle?.registrationNumber ?? "—"}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="incidentDate">Incident date</Label>
                <Controller
                  control={control}
                  name="incidentDate"
                  rules={{ required: true }}
                  render={({ field }) => (
                    <DatePicker id="incidentDate" value={field.value} onChange={field.onChange} />
                  )}
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="claimAmount">Claim amount</Label>
                <Input
                  id="claimAmount"
                  type="number"
                  min={0}
                  placeholder="50000"
                  {...register("claimAmount")}
                />
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="vehicleRegistrationNumber">Vehicle registration</Label>
              <Input
                id="vehicleRegistrationNumber"
                placeholder="MH 12 AB 1234"
                {...register("vehicleRegistrationNumber")}
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                rows={3}
                placeholder="What happened?"
                {...register("description")}
              />
            </div>

            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => router.back()}>
                Cancel
              </Button>
              <Button type="submit" disabled={create.loading || formState.isSubmitting}>
                {create.loading ? "Creating…" : "Create draft"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </>
  );
}
