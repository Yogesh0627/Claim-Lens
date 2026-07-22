package com.niyotechnologies.claimlens.notification.report;

/**
 * The claim lifecycle moments that trigger a rich email. Each carries the copy for that email —
 * headline and intro — while the coloured status badge is derived from the claim's current status,
 * not the event (see {@link ClaimReportRenderer}).
 */
public enum ClaimEmailEvent {

    SUBMITTED(
            "Claim received",
            "We've received your claim and started reviewing it. Here's a summary for your records — the full report is attached as a PDF."),
    ASSIGNED(
            "A claim is assigned to you",
            "This claim has completed automated processing and is ready for your investigation. The full report is attached."),
    INFO_REQUESTED(
            "We need a bit more information",
            "To continue with your claim we need an additional document. Details are below and in the attached report."),
    CUSTOMER_RESPONDED(
            "The customer has responded",
            "The policyholder uploaded new information — the claim is back under investigation. The current report is attached."),
    APPROVED(
            "Good news — your claim is approved",
            "Your claim has been approved. The full decision report, including the documents on file, is attached as a PDF."),
    REJECTED(
            "An update on your claim",
            "After investigation, your claim has not been approved. The reason and full report are below and attached.");

    private final String headline;
    private final String intro;

    ClaimEmailEvent(String headline, String intro) {
        this.headline = headline;
        this.intro = intro;
    }

    public String headline() {
        return headline;
    }

    public String intro() {
        return intro;
    }
}
