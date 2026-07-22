package com.niyotechnologies.claimlens.investigation.dto.request;

import com.niyotechnologies.claimlens.investigation.enums.NoteSeverity;
import com.niyotechnologies.claimlens.investigation.enums.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddNoteRequest(
        @NotNull(message = "Note type is required")
        NoteType noteType,

        @NotBlank(message = "Note is required")
        String note,

        NoteSeverity severity,

        Long documentId
) {
}
