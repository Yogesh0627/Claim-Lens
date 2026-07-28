"use client";

import { AiAssistant } from "@/components/ai/ai-assistant";
import { portalService } from "@/services/portalService";

/** The policyholder's assistant — scoped to their own policies. */
export function CustomerAssistant() {
  return (
    <AiAssistant
      contextLabel="Which policy?"
      loadContexts={async () => {
        const policies = await portalService.myPolicies();
        return policies.map((p) => ({
          id: p.id,
          label: p.policyNumber,
          sublabel: p.vehicle?.registrationNumber ?? undefined,
        }));
      }}
      ask={(policyId, question) => portalService.askCoverage(policyId, question)}
      suggestions={[
        "Is windshield damage covered?",
        "What do I need to claim for theft?",
        "What is my deductible?",
      ]}
      emptyContexts="No policies are linked to your account yet."
    />
  );
}
