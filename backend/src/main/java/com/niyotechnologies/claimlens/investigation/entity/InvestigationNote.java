package com.niyotechnologies.claimlens.investigation.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.investigation.enums.NoteSeverity;
import com.niyotechnologies.claimlens.investigation.enums.NoteType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A note recorded during investigation. Tenant-scoped; created_by (the author) is set by JPA auditing.
 * documentId optionally links the note to a piece of evidence (absorbs the old "Evidence" concept).
 */
@Entity
@Table(name = "investigation_note")
@Getter
@Setter
public class InvestigationNote extends TenantAwareEntity {

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 30)
    private NoteType noteType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private NoteSeverity severity;

    @Column(name = "document_id")
    private Long documentId;
}
