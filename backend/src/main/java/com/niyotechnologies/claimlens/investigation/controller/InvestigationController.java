package com.niyotechnologies.claimlens.investigation.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.investigation.dto.request.AddNoteRequest;
import com.niyotechnologies.claimlens.investigation.dto.response.NoteResponse;
import com.niyotechnologies.claimlens.investigation.service.InvestigationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/claims/{claimId}/investigation-notes")
@RequiredArgsConstructor
public class InvestigationController {

    @Autowired
    private final InvestigationService investigationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoteResponse> addNote(
            @PathVariable Long claimId,
            @Valid @RequestBody AddNoteRequest request) {
        return ApiResponse.success(investigationService.addNote(claimId, request));
    }

    @GetMapping
    public ApiResponse<List<NoteResponse>> listNotes(@PathVariable Long claimId) {
        return ApiResponse.success(investigationService.listNotes(claimId));
    }
}
