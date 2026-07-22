package com.niyotechnologies.claimlens.investigation.service.impl;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.claim.enums.ClaimStatus;
import com.niyotechnologies.claimlens.audit.annotation.Auditable;
import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.investigation.dto.request.AddNoteRequest;
import com.niyotechnologies.claimlens.investigation.dto.response.NoteResponse;
import com.niyotechnologies.claimlens.investigation.entity.InvestigationNote;
import com.niyotechnologies.claimlens.investigation.repository.InvestigationNoteRepository;
import com.niyotechnologies.claimlens.investigation.service.InvestigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestigationServiceImpl implements InvestigationService {

    @Autowired
    private final InvestigationNoteRepository noteRepository;
    @Autowired
    private final ClaimRepository claimRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('CLAIM_INVESTIGATE')")
    @Auditable(action = "INVESTIGATION_NOTE_ADDED", entityType = "CLAIM")
    public NoteResponse addNote(Long claimId, AddNoteRequest request) {
        Claim claim = claimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND", "Claim not found"));
        if (claim.getStatus() != ClaimStatus.UNDER_INVESTIGATION
                && claim.getStatus() != ClaimStatus.WAITING_FOR_CUSTOMER) {
            throw new BusinessException("CLAIM_NOT_UNDER_INVESTIGATION",
                    "Notes can only be added while the claim is under investigation");
        }

        InvestigationNote note = new InvestigationNote();
        note.setClaimId(claimId);
        note.setNoteType(request.noteType());
        note.setNote(request.note());
        note.setSeverity(request.severity());
        note.setDocumentId(request.documentId());
        return toResponse(noteRepository.save(note));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CLAIM_READ')")
    public List<NoteResponse> listNotes(Long claimId) {
        return noteRepository.findAllByClaimIdAndIsDeletedFalseOrderByCreatedAtAsc(claimId).stream()
                .map(this::toResponse)
                .toList();
    }

    private NoteResponse toResponse(InvestigationNote n) {
        return new NoteResponse(n.getId(), n.getClaimId(), n.getNoteType(), n.getNote(),
                n.getSeverity(), n.getDocumentId(), n.getCreatedBy(), n.getCreatedAt());
    }
}
