package com.niyotechnologies.claimlens.investigation.service;

import com.niyotechnologies.claimlens.investigation.dto.request.AddNoteRequest;
import com.niyotechnologies.claimlens.investigation.dto.response.NoteResponse;

import java.util.List;

public interface InvestigationService {

    NoteResponse addNote(Long claimId, AddNoteRequest request);

    List<NoteResponse> listNotes(Long claimId);
}
