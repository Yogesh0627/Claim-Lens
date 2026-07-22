package com.niyotechnologies.claimlens.processing.worker;

import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Fraud worker. Claims a job (its own transaction), then binds the tenant BEFORE the processing
 * transaction opens — because Hibernate resolves @TenantId at session open, a mid-transaction
 * TenantContext change would not scope the fraud engine's claim/policy queries.
 */
@Component
@RequiredArgsConstructor
public class FraudWorker {

    @Autowired
    private final FraudJobProcessor processor;

    public boolean pollOnce() {
        long[] ref = processor.claimNext();
        if (ref == null) {
            return false;
        }
        try {
            TenantContext.set(ref[2]);        // tenantId — bound before process() opens its session
            processor.process(ref[0], ref[1]); // jobId, claimId
        } finally {
            TenantContext.clear();
        }
        return true;
    }
}
