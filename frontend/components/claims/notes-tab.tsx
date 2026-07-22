"use client";

import { useState } from "react";
import { MessageSquarePlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { DataState, EmptyState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { Can } from "@/components/can";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { investigationService } from "@/services/investigationService";
import { NOTE_SEVERITIES, NOTE_TYPES, NOTE_TYPE_META, RISK_META, optionLabel } from "@/lib/enums";
import { formatDateTime } from "@/lib/dayjs";
import { PERMISSIONS } from "@/lib/permissions";

export function NotesTab({ claimId }: { claimId: number }) {
  const { data, loading, error, refetch } = useAsync(
    () => investigationService.list(claimId),
    [claimId],
  );

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Can permission={PERMISSIONS.CLAIM_INVESTIGATE}>
          <AddNoteDialog claimId={claimId} onAdded={refetch} />
        </Can>
      </div>
      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={
          <EmptyState title="No notes yet" description="Investigation notes will appear here." />
        }
      >
        {(notes) => (
          <div className="space-y-3">
            {notes.map((n) => (
              <Card key={n.id} className="space-y-2 p-4">
                <div className="flex items-center gap-2">
                  <StatusBadge value={n.noteType} map={NOTE_TYPE_META} />
                  {n.severity ? <StatusBadge value={n.severity} map={RISK_META} /> : null}
                  <span className="text-muted-foreground ml-auto text-xs">
                    {formatDateTime(n.createdAt)}
                  </span>
                </div>
                <p className="text-sm whitespace-pre-wrap">{n.note}</p>
              </Card>
            ))}
          </div>
        )}
      </DataState>
    </div>
  );
}

function AddNoteDialog({ claimId, onAdded }: { claimId: number; onAdded: () => void }) {
  const [open, setOpen] = useState(false);
  const [noteType, setNoteType] = useState<string>(NOTE_TYPES[NOTE_TYPES.length - 1]);
  const [severity, setSeverity] = useState<string>("");
  const [note, setNote] = useState("");

  const add = useMutation(
    () =>
      investigationService.add(claimId, {
        noteType,
        note,
        severity: severity || null,
      }),
    {
      successMessage: "Note added",
      onSuccess: () => {
        setOpen(false);
        setNote("");
        setSeverity("");
        onAdded();
      },
    },
  );

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <MessageSquarePlus className="mr-1 h-4 w-4" /> Add note
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add investigation note</DialogTitle>
        </DialogHeader>
        <div className="grid gap-4">
          <div className="grid gap-2">
            <Label>Type</Label>
            <Select value={noteType} onValueChange={setNoteType}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {NOTE_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>
                    {optionLabel(t)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid gap-2">
            <Label>Severity (optional)</Label>
            <Select value={severity} onValueChange={setSeverity}>
              <SelectTrigger>
                <SelectValue placeholder="None" />
              </SelectTrigger>
              <SelectContent>
                {NOTE_SEVERITIES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {optionLabel(s)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid gap-2">
            <Label>Note</Label>
            <Textarea rows={4} value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button onClick={() => add.run()} disabled={!note || add.loading}>
            {add.loading ? "Adding…" : "Add note"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
