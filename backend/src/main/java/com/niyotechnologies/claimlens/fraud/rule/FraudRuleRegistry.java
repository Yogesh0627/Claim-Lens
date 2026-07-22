package com.niyotechnologies.claimlens.fraud.rule;

import org.springframework.stereotype.Component;

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
}
