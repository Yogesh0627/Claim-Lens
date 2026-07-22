package com.niyotechnologies.claimlens.ruleset.entity;

import com.niyotechnologies.claimlens.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One configured rule in a fraud ruleset: a named check (code) with a weight and an on/off toggle. */
@Entity
@Table(name = "fraud_rule")
@Getter
@Setter
public class FraudRule extends TenantAwareEntity {

    @Column(name = "fraud_ruleset_id", nullable = false)
    private Long fraudRulesetId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private Boolean enabled = true;
}
