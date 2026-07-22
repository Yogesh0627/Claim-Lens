package com.niyotechnologies.claimlens.ruleset.service.impl;

import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.product.repository.ClaimTypeRepository;
import com.niyotechnologies.claimlens.ruleset.dto.request.CreateFraudRulesetRequest;
import com.niyotechnologies.claimlens.ruleset.dto.request.FraudRuleInput;
import com.niyotechnologies.claimlens.ruleset.dto.response.FraudRulesetResponse;
import com.niyotechnologies.claimlens.ruleset.entity.FraudRule;
import com.niyotechnologies.claimlens.ruleset.entity.FraudRuleset;
import com.niyotechnologies.claimlens.ruleset.enums.RulesetStatus;
import com.niyotechnologies.claimlens.ruleset.mapper.FraudRulesetMapper;
import com.niyotechnologies.claimlens.ruleset.repository.FraudRuleRepository;
import com.niyotechnologies.claimlens.ruleset.repository.FraudRulesetRepository;
import com.niyotechnologies.claimlens.ruleset.service.RulesetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RulesetServiceImpl implements RulesetService {

    @Autowired
    private final FraudRulesetRepository rulesetRepository;
    @Autowired
    private final FraudRuleRepository ruleRepository;
    @Autowired
    private final ClaimTypeRepository claimTypeRepository;
    @Autowired
    private final FraudRulesetMapper mapper;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('RULESET_WRITE')")
    public FraudRulesetResponse createFraudRuleset(CreateFraudRulesetRequest request) {
        Long claimTypeId = claimTypeRepository.findByCode(request.claimTypeCode())
                .orElseThrow(() -> new NotFoundException("CLAIM_TYPE_NOT_FOUND", "Claim type not found"))
                .getId();

        FraudRuleset ruleset = rulesetRepository.save(mapper.toEntity(request, claimTypeId));

        List<FraudRule> rules = request.rules() == null ? List.of()
                : request.rules().stream()
                    .map((FraudRuleInput input) -> ruleRepository.save(mapper.toRule(input, ruleset.getId())))
                    .toList();

        return mapper.toResponse(ruleset, rules);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('RULESET_WRITE')")
    public FraudRulesetResponse activateFraudRuleset(Long rulesetId) {
        FraudRuleset ruleset = getRulesetOrThrow(rulesetId);

        // Retire the currently-active ruleset for this claim type (one-active-per-type index).
        rulesetRepository.findByClaimTypeIdAndStatusAndIsDeletedFalse(
                ruleset.getClaimTypeId(), RulesetStatus.ACTIVE).ifPresent(active -> {
            if (!active.getId().equals(ruleset.getId())) {
                active.setStatus(RulesetStatus.RETIRED);
                rulesetRepository.saveAndFlush(active);
            }
        });

        ruleset.setStatus(RulesetStatus.ACTIVE);
        rulesetRepository.save(ruleset);
        return mapper.toResponse(ruleset, ruleRepository.findAllByFraudRulesetIdAndIsDeletedFalse(rulesetId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('RULESET_READ')")
    public FraudRulesetResponse getFraudRuleset(Long rulesetId) {
        FraudRuleset ruleset = getRulesetOrThrow(rulesetId);
        return mapper.toResponse(ruleset, ruleRepository.findAllByFraudRulesetIdAndIsDeletedFalse(rulesetId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('RULESET_READ')")
    public List<FraudRulesetResponse> getFraudRulesets() {
        return rulesetRepository.findAllByIsDeletedFalse().stream()
                .map(rs -> mapper.toResponse(rs,
                        ruleRepository.findAllByFraudRulesetIdAndIsDeletedFalse(rs.getId())))
                .toList();
    }

    private FraudRuleset getRulesetOrThrow(Long rulesetId) {
        return rulesetRepository.findByIdAndIsDeletedFalse(rulesetId)
                .orElseThrow(() -> new NotFoundException("FRAUD_RULESET_NOT_FOUND", "Fraud ruleset not found"));
    }
}
