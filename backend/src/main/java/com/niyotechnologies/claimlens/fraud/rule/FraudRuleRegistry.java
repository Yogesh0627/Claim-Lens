package com.niyotechnologies.claimlens.fraud.rule;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Looks up a rule evaluator by its code. All FraudRuleEvaluator beans are registered automatically. */
@Component
public class FraudRuleRegistry {

    private final Map<String, FraudRuleEvaluator> byCode;

    public FraudRuleRegistry(List<FraudRuleEvaluator> evaluators) {
        this.byCode = evaluators.stream()
                .collect(Collectors.toMap(FraudRuleEvaluator::code, Function.identity()));
    }

    public FraudRuleEvaluator get(String code) {
        return byCode.get(code);
    }

    /** All implemented rules (code + description + suggested weight), sorted by code, for the config UI. */
    public List<FraudRuleCatalogEntry> catalog() {
        return byCode.values().stream()
                .map(e -> new FraudRuleCatalogEntry(e.code(), e.description(), e.defaultWeight()))
                .sorted(Comparator.comparing(FraudRuleCatalogEntry::code))
                .toList();
    }
}
