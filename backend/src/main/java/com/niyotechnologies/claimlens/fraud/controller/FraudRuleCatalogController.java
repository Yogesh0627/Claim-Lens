package com.niyotechnologies.claimlens.fraud.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleCatalogEntry;
import com.niyotechnologies.claimlens.fraud.rule.FraudRuleRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The catalog of IMPLEMENTED fraud rules. The ruleset config UI reads this so an admin picks real
 * rules from a list instead of typing free-text codes — a code with no matching evaluator would be
 * silently ignored by the engine, which is the trap this endpoint removes.
 */
@RestController
@RequestMapping("${claimlens.api.base-path}/rulesets/fraud/rule-catalog")
@RequiredArgsConstructor
public class FraudRuleCatalogController {

    @Autowired
    private final FraudRuleRegistry ruleRegistry;

    @GetMapping
    @PreAuthorize("hasAuthority('RULESET_READ')")
    public ApiResponse<List<FraudRuleCatalogEntry>> catalog() {
        return ApiResponse.success(ruleRegistry.catalog());
    }
}
