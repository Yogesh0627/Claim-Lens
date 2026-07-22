"use client";

import { useRef, useState } from "react";
import { Download, FileText, History, Upload, RotateCcw } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card } from "@/components/ui/card";
import { DataState, EmptyState } from "@/components/data-state";
import { StatusBadge } from "@/components/status-badge";
import { IconButton } from "@/components/icon-button";
import { Can } from "@/components/can";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { documentService } from "@/services/documentService";
import { DOCUMENT_TYPES, GENERIC_STATUS_META, optionLabel } from "@/lib/enums";
import { formatBytes } from "@/lib/format";
import { formatDateTime } from "@/lib/dayjs";
import { isApiError } from "@/lib/http";
import { PERMISSIONS } from "@/lib/permissions";
import type { DocumentResponse } from "@/lib/types";

async function openBlob(loadUrl: () => Promise<string>, failMessage: string) {
  try {
    const url = await loadUrl();
    window.open(url, "_blank", "noopener");
  } catch (e) {
    toast.error(isApiError(e) ? e.message : failMessage);
  }
}

function DocumentRow({
  claimId,
  doc,
  onChanged,
}: {
  claimId: number;
  doc: DocumentResponse;
  onChanged: () => void;
}) {
  const [showHistory, setShowHistory] = useState(false);
  const reviseInput = useRef<HTMLInputElement>(null);
  const versions = useAsync(
    () => documentService.listVersions(claimId, doc.id),
    [claimId, doc.id, showHistory],
    { enabled: showHistory },
  );

  const revise = useMutation(
    (file: File) => documentService.uploadVersion(claimId, doc.id, file),
    {
      successMessage: "New version uploaded",
      onSuccess: () => {
        onChanged();
        if (showHistory) versions.refetch();
      },
    },
  );

  const onPickRevision = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) revise.run(file);
    if (reviseInput.current) reviseInput.current.value = "";
  };

  return (
    <Card className="p-3">
      <div className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          <FileText className="text-muted-foreground h-5 w-5 shrink-0" />
          <div className="min-w-0">
            <p className="truncate font-medium">{doc.fileName}</p>
            <p className="text-muted-foreground text-xs">
              {optionLabel(doc.documentType)} · {formatBytes(doc.sizeBytes)} ·{" "}
              {formatDateTime(doc.createdAt)}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge value={doc.status} map={GENERIC_STATUS_META} />
          <IconButton
            label="Version history"
            onClick={() => setShowHistory((v) => !v)}
          >
            <History className="h-4 w-4" />
          </IconButton>
          <Can permission={PERMISSIONS.CLAIM_WRITE}>
            <input
              ref={reviseInput}
              type="file"
              className="hidden"
              onChange={onPickRevision}
            />
            <IconButton
              label="Upload new version"
              onClick={() => reviseInput.current?.click()}
              disabled={revise.loading}
            >
              <RotateCcw className="h-4 w-4" />
            </IconButton>
          </Can>
          <IconButton
            label="View / download"
            onClick={() =>
              openBlob(
                () => documentService.downloadUrl(claimId, doc.id),
                "Could not open document",
              )
            }
          >
            <Download className="h-4 w-4" />
          </IconButton>
        </div>
      </div>

      {showHistory ? (
        <div className="mt-3 border-t pt-3">
          <DataState
            loading={versions.loading}
            error={versions.error}
            data={versions.data}
            emptyWhen={(d) => d.length === 0}
            empty={<p className="text-muted-foreground text-sm">No versions yet.</p>}
          >
            {(list) => (
              <ul className="space-y-1">
                {list.map((v) => (
                  <li
                    key={v.id}
                    className="flex items-center justify-between gap-3 text-sm"
                  >
                    <span className="flex min-w-0 items-center gap-2">
                      <span className="text-muted-foreground w-8 shrink-0 font-mono">
                        v{v.versionNumber}
                      </span>
                      <span className="truncate">{v.fileName}</span>
                      {v.current ? (
                        <span className="bg-primary/10 text-primary rounded px-1.5 py-0.5 text-[10px] font-medium">
                          current
                        </span>
                      ) : null}
                    </span>
                    <span className="flex shrink-0 items-center gap-2">
                      <span className="text-muted-foreground text-xs">
                        {formatDateTime(v.createdAt)}
                      </span>
                      <IconButton
                        label={`Download v${v.versionNumber}`}
                        onClick={() =>
                          openBlob(
                            () =>
                              documentService.versionDownloadUrl(claimId, doc.id, v.id),
                            "Could not open version",
                          )
                        }
                      >
                        <Download className="h-4 w-4" />
                      </IconButton>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </DataState>
        </div>
      ) : null}
    </Card>
  );
}

export function DocumentsTab({ claimId }: { claimId: number }) {
  const { data, loading, error, refetch } = useAsync(
    () => documentService.list(claimId),
    [claimId],
  );
  const [docType, setDocType] = useState<string>(DOCUMENT_TYPES[0]);
  const fileInput = useRef<HTMLInputElement>(null);

  const upload = useMutation((file: File) => documentService.upload(claimId, docType, file), {
    successMessage: "Document uploaded",
    onSuccess: () => refetch(),
  });

  const onPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) upload.run(file);
    if (fileInput.current) fileInput.current.value = "";
  };

  return (
    <div className="space-y-4">
      <Can permission={PERMISSIONS.CLAIM_WRITE}>
        <Card className="flex flex-col gap-3 p-4 sm:flex-row sm:items-end">
          <div className="grid flex-1 gap-2">
            <Label>Document type</Label>
            <Select value={docType} onValueChange={setDocType}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {DOCUMENT_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>
                    {optionLabel(t)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <input ref={fileInput} type="file" className="hidden" onChange={onPick} />
          <Button onClick={() => fileInput.current?.click()} disabled={upload.loading}>
            <Upload className="mr-1 h-4 w-4" /> {upload.loading ? "Uploading…" : "Upload"}
          </Button>
        </Card>
      </Can>

      <DataState
        loading={loading}
        error={error}
        data={data}
        emptyWhen={(d) => d.length === 0}
        empty={
          <EmptyState title="No documents" description="Upload the claim's supporting files." />
        }
      >
        {(docs) => (
          <div className="space-y-2">
            {docs.map((doc) => (
              <DocumentRow key={doc.id} claimId={claimId} doc={doc} onChanged={refetch} />
            ))}
          </div>
        )}
      </DataState>
    </div>
  );
}
