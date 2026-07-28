package com.niyotechnologies.claimlens.fraud.controller;

import com.niyotechnologies.claimlens.claim.entity.Claim;
import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.fraud.dto.FraudLabelResponse;
import com.niyotechnologies.claimlens.fraud.repository.FraudScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The fraud-evaluation feedback loop: emits the labelled dataset of DECIDED claims — the engine's score
 * and fired rules paired with the investigator's ground-truth fraud verdict — so the evaluation harness
 * can compute precision/recall/AUC on real usage instead of synthetic data. Read-only, FRAUD_READ.
 */
@RestController
@RequestMapping("${claimlens.api.base-path}/fraud/evaluation")
@RequiredArgsConstructor
public class FraudEvaluationController {

    /** Pulls "CODE" out of the engine's "CODE (+40); ..." explanation string. */
    private static final Pattern RULE = Pattern.compile("([A-Z_]+)\\s*\\(\\+\\d+\\)");

    @Autowired
    private final ClaimRepository claimRepository;
    @Autowired
    private final FraudScoreRepository fraudScoreRepository;

    @GetMapping("/dataset")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('FRAUD_READ')")
    public ApiResponse<List<FraudLabelResponse>> dataset() {
        List<FraudLabelResponse> rows = new ArrayList<>();
        for (Claim claim : claimRepository.findAllByFraudConfirmedIsNotNullAndIsDeletedFalse()) {
            fraudScoreRepository.findFirstByClaimIdOrderByCreatedAtDesc(claim.getId())
                    .ifPresent(fs -> rows.add(new FraudLabelResponse(
                            claim.getId(), claim.getClaimNumber(),
                            fs.getScore(), fs.getRiskLevel(),
                            firedRules(fs.getExplanation()),
                            Boolean.TRUE.equals(claim.getFraudConfirmed()))));
        }
        return ApiResponse.success(rows);
    }

    private static List<String> firedRules(String explanation) {
        List<String> codes = new ArrayList<>();
        if (explanation != null) {
            Matcher m = RULE.matcher(explanation);
            while (m.find()) {
                codes.add(m.group(1));
            }
        }
        return codes;
    }
}
