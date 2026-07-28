"use client";

import { useState } from "react";
import {
  CheckCircle2,
  MessageSquare,
  RefreshCw,
  Send,
  UserPlus,
  Wand2,
  XCircle,
} from "lucide-react";
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
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Can } from "@/components/can";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { claimService } from "@/services/claimService";
import { userService } from "@/services/userService";
import { PERMISSIONS } from "@/lib/permissions";
import type { ClaimResponse } from "@/lib/types";

const TERMINAL = ["APPROVED", "REJECTED", "CLOSED"];

export function ClaimActions({
  claim,
  onChanged,
}: {
  claim: ClaimResponse;
  onChanged: () => void;
}) {
  const isDraft = claim.status === "DRAFT";
  const isTerminal = TERMINAL.includes(claim.status);
  const canAct = !isDraft && !isTerminal;

  const submit = useMutation(() => claimService.submit(claim.id), {
    successMessage: "Claim submitted",
    onSuccess: onChanged,
  });
  const autoAssign = useMutation(() => claimService.autoAssign(claim.id), {
    successMessage: "Auto-assigned to the least-loaded investigator",
    onSuccess: onChanged,
  });

  return (
    <div className="flex flex-wrap items-center gap-2">
      {isDraft ? (
        <Can permission={PERMISSIONS.CLAIM_SUBMIT}>
          <Button onClick={() => submit.run()} disabled={submit.loading}>
            <Send className="mr-1 h-4 w-4" /> {submit.loading ? "Submitting…" : "Submit"}
          </Button>
        </Can>
      ) : null}

      {claim.status === "AWAITING_ASSIGNMENT" ? (
        <Can permission={PERMISSIONS.CLAIM_ASSIGN}>
          <Button variant="outline" onClick={() => autoAssign.run()} disabled={autoAssign.loading}>
            <Wand2 className="mr-1 h-4 w-4" /> Auto-assign
          </Button>
          <AssignDialog claimId={claim.id} onChanged={onChanged} />
        </Can>
      ) : null}

      {claim.status === "UNDER_INVESTIGATION" ? (
        <>
          <Can permission={PERMISSIONS.CLAIM_ASSIGN}>
            <ReassignDialog claimId={claim.id} onChanged={onChanged} />
          </Can>
          <Can permission={PERMISSIONS.CLAIM_INVESTIGATE}>
            <RequestInfoDialog claimId={claim.id} onChanged={onChanged} />
          </Can>
        </>
      ) : null}

      {canAct ? (
        <Can permission={PERMISSIONS.CLAIM_DECIDE}>
          <DecideDialog claimId={claim.id} onChanged={onChanged} />
        </Can>
      ) : null}
    </div>
  );
}

function RequestInfoDialog({ claimId, onChanged }: { claimId: number; onChanged: () => void }) {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");
  const request = useMutation(() => claimService.requestInformation(claimId, message), {
    successMessage: "Information requested — the customer has been notified",
    onSuccess: () => {
      setOpen(false);
      setMessage("");
      onChanged();
    },
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline">
          <MessageSquare className="mr-1 h-4 w-4" /> Request info
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Request more information</DialogTitle>
          <DialogDescription>
            Tell the policyholder what&apos;s needed. They&apos;re emailed a branded report, and the
            claim moves to &ldquo;Waiting for customer.&rdquo;
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-2">
          <Label htmlFor="request-message">Message to the customer</Label>
          <Textarea
            id="request-message"
            rows={3}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="e.g. Please upload a clearer photo of your RC book."
          />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button onClick={() => request.run()} disabled={!message.trim() || request.loading}>
            {request.loading ? "Sending…" : "Send request"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function AssignDialog({ claimId, onChanged }: { claimId: number; onChanged: () => void }) {
  const [open, setOpen] = useState(false);
  const [userId, setUserId] = useState("");
  // Load the tenant's investigators. If the caller lacks USER_READ this errors and we fall back to
  // a numeric ID field, so assignment still works.
  const { data: investigators, error: listError } = useAsync(
    () => (open ? userService.options("INVESTIGATOR") : Promise.resolve([])),
    [open],
  );
  const assign = useMutation(
    (investigatorUserId: number) => claimService.assign(claimId, { investigatorUserId }),
    {
      successMessage: "Claim assigned",
      onSuccess: () => {
        setOpen(false);
        setUserId("");
        onChanged();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline">
          <UserPlus className="mr-1 h-4 w-4" /> Assign
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Assign investigator</DialogTitle>
          <DialogDescription>Choose an investigator to assign this claim to.</DialogDescription>
        </DialogHeader>
        <div className="grid gap-2">
          <Label htmlFor="investigator">Investigator</Label>
          {listError ? (
            <Input
              id="investigator"
              type="number"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="Investigator user ID"
            />
          ) : (
            <Select value={userId} onValueChange={setUserId}>
              <SelectTrigger id="investigator">
                <SelectValue placeholder="Select an investigator" />
              </SelectTrigger>
              <SelectContent>
                {(investigators ?? []).map((u) => (
                  <SelectItem key={u.id} value={String(u.id)}>
                    {u.firstName} {u.lastName ?? ""} · {u.employeeCode}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button onClick={() => assign.run(Number(userId))} disabled={!userId || assign.loading}>
            {assign.loading ? "Assigning…" : "Assign"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ReassignDialog({ claimId, onChanged }: { claimId: number; onChanged: () => void }) {
  const [open, setOpen] = useState(false);
  const [userId, setUserId] = useState("auto");
  const { data: investigators, error: listError } = useAsync(
    () => (open ? userService.options("INVESTIGATOR") : Promise.resolve([])),
    [open],
  );
  const reassign = useMutation(
    () => claimService.reassign(claimId, userId === "auto" || !userId ? null : Number(userId)),
    {
      successMessage: "Claim reassigned",
      onSuccess: () => {
        setOpen(false);
        setUserId("auto");
        onChanged();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline">
          <RefreshCw className="mr-1 h-4 w-4" /> Reassign
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Reassign claim</DialogTitle>
          <DialogDescription>
            Move this claim to a different investigator. Choose &ldquo;Auto&rdquo; to route it to the
            least-loaded one.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-2">
          <Label htmlFor="reassign-investigator">Investigator</Label>
          {listError ? (
            <Input
              id="reassign-investigator"
              type="number"
              value={userId === "auto" ? "" : userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="Investigator user ID (blank = auto)"
            />
          ) : (
            <Select value={userId} onValueChange={setUserId}>
              <SelectTrigger id="reassign-investigator">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="auto">Auto — least-loaded investigator</SelectItem>
                {(investigators ?? []).map((u) => (
                  <SelectItem key={u.id} value={String(u.id)}>
                    {u.firstName} {u.lastName ?? ""} · {u.employeeCode}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button onClick={() => reassign.run()} disabled={reassign.loading}>
            {reassign.loading ? "Reassigning…" : "Reassign"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function DecideDialog({ claimId, onChanged }: { claimId: number; onChanged: () => void }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [fraud, setFraud] = useState(false);
  const decide = useMutation(
    (decision: "APPROVE" | "REJECT") =>
      claimService.decide(claimId, {
        decision,
        reason: reason || null,
        // The fraud verdict is only meaningful on a rejection; an approval is recorded as not-fraud.
        fraudConfirmed: decision === "REJECT" ? fraud : false,
      }),
    {
      successMessage: "Decision recorded",
      onSuccess: () => {
        setOpen(false);
        setReason("");
        setFraud(false);
        onChanged();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>Decide</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Record decision</DialogTitle>
          <DialogDescription>
            Approve or reject this claim with an optional reason.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-3">
          <div className="grid gap-2">
            <Label htmlFor="reason">Reason</Label>
            <Textarea
              id="reason"
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Rationale for the decision"
            />
          </div>
          {/* Ground-truth label for the fraud-model evaluation loop — set it when rejecting for fraud. */}
          <label className="flex cursor-pointer items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="accent-primary mt-0.5 h-4 w-4"
              checked={fraud}
              onChange={(e) => setFraud(e.target.checked)}
            />
            <span>
              This claim was <b>fraudulent</b>
              <span className="text-muted-foreground block text-xs">
                Tick only if rejecting because of confirmed fraud (not a coverage reason). Trains the
                fraud-score evaluation.
              </span>
            </span>
          </label>
        </div>
        <DialogFooter className="sm:justify-between">
          <Button
            variant="destructive"
            onClick={() => decide.run("REJECT")}
            disabled={decide.loading}
          >
            <XCircle className="mr-1 h-4 w-4" /> Reject
          </Button>
          <Button
            className="bg-emerald-600 hover:bg-emerald-700"
            onClick={() => decide.run("APPROVE")}
            disabled={decide.loading}
          >
            <CheckCircle2 className="mr-1 h-4 w-4" /> Approve
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
