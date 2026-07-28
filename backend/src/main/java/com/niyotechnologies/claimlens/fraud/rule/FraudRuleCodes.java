package com.niyotechnologies.claimlens.fraud.rule;

public final class FraudRuleCodes {

    public static final String AMOUNT_OVER_SUM_INSURED = "AMOUNT_OVER_SUM_INSURED";
    public static final String EARLY_CLAIM = "EARLY_CLAIM";
    // Image-forensics rules — fed by the analysis service's signals (previously produced but unused).
    public static final String DUPLICATE_IMAGE = "DUPLICATE_IMAGE";
    public static final String EXIF_INCONSISTENT = "EXIF_INCONSISTENT";
    public static final String SYNTHETIC_IMAGE = "SYNTHETIC_IMAGE";

    private FraudRuleCodes() {
    }
}
