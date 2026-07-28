"use client";

import { AiAssistant } from "@/components/ai/ai-assistant";
import { coverageService } from "@/services/coverageService";

/** The staff assistant — pick any product that has ingested wording and ask about its coverage. */
export function StaffAssistant() {
  return (
    <AiAssistant
      contextLabel="Which product?"
      loadContexts={async () => {
        const products = await coverageService.askableProducts();
        return products.map((p) => ({
          id: p.versionId,
          label: p.productName,
          sublabel: `v${p.versionNumber}`,
        }));
      }}
      ask={(versionId, question) =>
        coverageService.ask({ insuranceProductVersionId: versionId, question })
      }
      suggestions={[
        "Is windshield damage covered?",
        "What are the main exclusions?",
        "What is required to claim for theft?",
      ]}
      emptyContexts="No product wording is ingested yet. Add it from a product's Knowledge or Wording action."
    />
  );
}
