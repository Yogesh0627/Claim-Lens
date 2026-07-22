package com.niyotechnologies.claimlens.ruleset.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import com.niyotechnologies.claimlens.ruleset.enums.RulesetStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Admin-configurable fraud rules for a (tenant, claim_type). One may be ACTIVE at a time. */
@Entity
@Table(name = "fraud_ruleset")
@Getter
@Setter
public class FraudRuleset extends TenantAwareEntity {

    @Column(name = "claim_type_id", nullable = false)
    private Long claimTypeId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "medium_threshold", nullable = false)
    private Integer mediumThreshold;

    @Column(name = "high_threshold", nullable = false)
    private Integer highThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RulesetStatus status;
}
