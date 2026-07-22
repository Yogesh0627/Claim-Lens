package com.niyotechnologies.claimlens.ruleset.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.ruleset.dto.request.CreateFraudRulesetRequest;
import com.niyotechnologies.claimlens.ruleset.dto.response.FraudRulesetResponse;
import com.niyotechnologies.claimlens.ruleset.service.RulesetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Config-rules endpoints. Named /rulesets (D2) — "policy" is reserved for the insurance contract. */
@RestController
@RequestMapping("${claimlens.api.base-path}/rulesets/fraud")
@RequiredArgsConstructor
public class RulesetController {

    @Autowired
    private final RulesetService rulesetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FraudRulesetResponse> create(@Valid @RequestBody CreateFraudRulesetRequest request) {
        return ApiResponse.success(rulesetService.createFraudRuleset(request));
    }

    @PostMapping("/{rulesetId}/activate")
    public ApiResponse<FraudRulesetResponse> activate(@PathVariable Long rulesetId) {
        return ApiResponse.success(rulesetService.activateFraudRuleset(rulesetId));
    }

    @GetMapping("/{rulesetId}")
    public ApiResponse<FraudRulesetResponse> get(@PathVariable Long rulesetId) {
        return ApiResponse.success(rulesetService.getFraudRuleset(rulesetId));
    }

    @GetMapping
    public ApiResponse<List<FraudRulesetResponse>> list() {
        return ApiResponse.success(rulesetService.getFraudRulesets());
    }
}
