"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
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
import { portalService } from "@/services/portalService";
import type { FileClaimRequest } from "@/lib/types";

interface ClaimForm {
  insurancePolicyId: string;
  incidentDate: string;
  claimAmount: string;
  vehicleRegistrationNumber: string;
  description: string;
}

export default function PortalNewClaimPage() {
  const router = useRouter();
  const { register, handleSubmit, watch, setValue, control, formState } = useForm<ClaimForm>({
    defaultValues: {
      insurancePolicyId: "",
      incidentDate: "",
      claimAmount: "",
      vehicleRegistrationNumber: "",
      description: "",
    },
  });

  const { data: policies } = useAsync(() => portalService.myPolicies(), []);
  const policyId = watch("insurancePolicyId");

  // Prefill the vehicle registration from the chosen policy.
  const [prefilledFor, setPrefilledFor] = useState<string>("");
  const selectedPolicy = (policies ?? []).find((p) => String(p.id) === policyId);
  if (selectedPolicy && prefilledFor !== policyId) {
    setPrefilledFor(policyId);
    setValue("vehicleRegistrationNumber", selectedPolicy.vehicle?.registrationNumber ?? "");
  }

  const create = useMutation(portalService.fileClaim, {
    successMessage: "Draft claim created",
    onSuccess: (claim) => router.replace(`/portal/claims/${claim.id}`),
  });

  const onSubmit = (values: ClaimForm) => {
    const body: FileClaimRequest = {
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
      <Button variant="ghost" size="sm" className="-ml-2 w-fit" onClick={() => router.push("/portal")}>
        <ArrowLeft className="mr-1 h-4 w-4" /> My claims
      </Button>
      <PageHeader
        title="File a claim"
        description="Pick the policy involved, tell us what happened, then upload documents and submit."
      />
      <Card className="max-w-2xl">
        <CardContent className="pt-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div className="grid gap-2">
              <Label htmlFor="insurancePolicyId">Policy</Label>
              <Select value={policyId} onValueChange={(v) => setValue("insurancePolicyId", v)}>
                <SelectTrigger id="insurancePolicyId">
                  <SelectValue placeholder="Select a policy" />
                </SelectTrigger>
                <SelectContent>
                  {(policies ?? []).map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.policyNumber} · {p.vehicle?.registrationNumber ?? "—"}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
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
                <Label htmlFor="claimAmount">Estimated amount</Label>
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
              <Label htmlFor="description">What happened?</Label>
              <Textarea
                id="description"
                rows={3}
                placeholder="Describe the incident"
                {...register("description")}
              />
            </div>

            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => router.push("/portal")}>
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={create.loading || formState.isSubmitting || !policyId}
              >
                {create.loading ? "Creating…" : "Create draft"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </>
  );
}
