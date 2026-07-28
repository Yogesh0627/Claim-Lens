package com.niyotechnologies.claimlens.fraud.rule;

/** One implemented fraud rule, surfaced so the config UI offers real rules instead of free-text codes. */
public record FraudRuleCatalogEntry(String code, String description, int defaultWeight) {
}
