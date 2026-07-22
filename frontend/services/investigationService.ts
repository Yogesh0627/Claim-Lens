import { http } from "@/lib/http";
import type { AddNoteRequest, ApiResponse, NoteResponse } from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const investigationService = {
  list: (claimId: number) =>
    unwrap(http.get<ApiResponse<NoteResponse[]>>(`/claims/${claimId}/investigation-notes`)),
  add: (claimId: number, body: AddNoteRequest) =>
    unwrap(http.post<ApiResponse<NoteResponse>>(`/claims/${claimId}/investigation-notes`, body)),
};
