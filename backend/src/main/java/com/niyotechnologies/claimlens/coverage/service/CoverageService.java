package com.niyotechnologies.claimlens.coverage.service;

import com.niyotechnologies.claimlens.claim.repository.ClaimRepository;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.coverage.ai.ChatClient;
import com.niyotechnologies.claimlens.coverage.ai.EmbeddingClient;
import com.niyotechnologies.claimlens.coverage.dto.AskCoverageRequest;
import com.niyotechnologies.claimlens.coverage.dto.AskCoverageResponse;
import com.niyotechnologies.claimlens.coverage.dto.IngestKnowledgeRequest;
import com.niyotechnologies.claimlens.coverage.dto.IngestKnowledgeResponse;
import com.niyotechnologies.claimlens.coverage.entity.CoverageAnswer;
import com.niyotechnologies.claimlens.coverage.entity.CoverageCitation;
import com.niyotechnologies.claimlens.coverage.entity.PolicyChunk;
import com.niyotechnologies.claimlens.coverage.repository.CoverageAnswerRepository;
import com.niyotechnologies.claimlens.coverage.repository.CoverageCitationRepository;
import com.niyotechnologies.claimlens.coverage.repository.PolicyChunkRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductVersionRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductRepository;
import com.niyotechnologies.claimlens.coverage.dto.AskableProductResponse;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Policy-intelligence RAG. Ingest chunks + embeds policy wording per product VERSION; ask embeds the
 * question, retrieves the most similar chunks (cosine, in-process — no pgvector at V1), and asks the
 * chat client for a grounded answer with citations. All reads are @TenantId-scoped, and retrieval is
 * filtered to the pinned product version, so answers reflect the contracted terms and never leak
 * across tenants (D7).
 */
@Service
@RequiredArgsConstructor
public class CoverageService {

    private static final int TOP_K = 5;
    private static final int SNIPPET_CHARS = 240;

    @Autowired
    private final InsuranceProductVersionRepository productVersionRepository;
    @Autowired
    private final InsuranceProductRepository productRepository;
    @Autowired
    private final ClaimRepository claimRepository;
    @Autowired
    private final PolicyChunkRepository policyChunkRepository;
    @Autowired
    private final CoverageAnswerRepository coverageAnswerRepository;
    @Autowired
    private final CoverageCitationRepository citationRepository;
    @Autowired
    private final EmbeddingClient embeddingClient;
    @Autowired
    private final ChatClient chatClient;
    @Autowired
    private final CoverageVectorSearch vectorSearch;

    @Transactional
    @PreAuthorize("hasAuthority('COVERAGE_WRITE')")
    public IngestKnowledgeResponse ingest(Long productId, Long versionId, IngestKnowledgeRequest request) {
        return ingestText(productId, versionId, request.text());
    }

    /**
     * The chunk + embed + store logic, without the COVERAGE_WRITE guard, so an already-authorized
     * caller can reuse it — e.g. a product-document (wording PDF) upload that was authorized under
     * PRODUCT_WRITE and wants to auto-populate the AI knowledge from the extracted text. Callers that
     * are NOT already authorized must go through {@link #ingest} instead.
     */
    @Transactional
    public IngestKnowledgeResponse ingestText(Long productId, Long versionId, String text) {
        var version = productVersionRepository.findByIdAndIsDeletedFalse(versionId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_VERSION_NOT_FOUND", "Product version not found"));
        if (!version.getInsuranceProductId().equals(productId)) {
            throw new NotFoundException("PRODUCT_VERSION_NOT_FOUND", "Product version not found");
        }

        // Re-ingest replaces the version's knowledge. Past answers may cite the old chunks; those
        // citations reference wording that no longer exists, so drop them first to avoid an FK
        // violation when the chunks are deleted.
        List<Long> oldChunkIds = policyChunkRepository.findAllByInsuranceProductVersionId(versionId)
                .stream().map(PolicyChunk::getId).toList();
        if (!oldChunkIds.isEmpty()) {
            citationRepository.deleteByPolicyChunkIdIn(oldChunkIds);
        }
        policyChunkRepository.deleteByInsuranceProductVersionId(versionId);

        List<String> chunks = TextChunker.chunk(text);
        int index = 0;
        for (String chunkText : chunks) {
            float[] embedding = embeddingClient.embed(chunkText);
            PolicyChunk chunk = new PolicyChunk();
            chunk.setInsuranceProductId(productId);
            chunk.setInsuranceProductVersionId(versionId);
            chunk.setChunkIndex(index++);
            chunk.setChunkText(chunkText);
            chunk.setEmbedding(Embeddings.serialize(embedding));
            chunk.setEmbeddingModel(embeddingClient.model());
            PolicyChunk saved = policyChunkRepository.save(chunk);
            vectorSearch.index(saved, embedding);   // populates the pgvector column when enabled
        }
        return new IngestKnowledgeResponse(versionId, chunks.size(), embeddingClient.model());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('COVERAGE_READ')")
    public IngestKnowledgeResponse status(Long productId, Long versionId) {
        var version = productVersionRepository.findByIdAndIsDeletedFalse(versionId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_VERSION_NOT_FOUND", "Product version not found"));
        if (!version.getInsuranceProductId().equals(productId)) {
            throw new NotFoundException("PRODUCT_VERSION_NOT_FOUND", "Product version not found");
        }
        long count = policyChunkRepository.countByInsuranceProductVersionId(versionId);
        return new IngestKnowledgeResponse(versionId, count, count > 0 ? embeddingClient.model() : null);
    }

    /**
     * Coverage explanations are non-sensitive product wording, so any authenticated STAFF member may
     * ask — the assistant is meant to help every role understand the policy. Customers are excluded
     * here (they hold PORTAL_CLAIM_READ) and go through the ownership-scoped
     * {@code POST /portal/coverage/ask} instead, so they can only ask about their OWN policies.
     */
    @Transactional
    @PreAuthorize("isAuthenticated() and !hasAuthority('PORTAL_CLAIM_READ')")
    public AskCoverageResponse ask(AskCoverageRequest request) {
        return askInternal(request);
    }

    /** The ask body without the staff guard — reached from the customer portal after an ownership check. */
    @Transactional
    public AskCoverageResponse askInternal(AskCoverageRequest request) {
        Long versionId = resolveVersionId(request);
        // Verify the version is visible to this tenant (@TenantId) — else 404, never another tenant's data.
        productVersionRepository.findByIdAndIsDeletedFalse(versionId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_VERSION_NOT_FOUND", "Product version not found"));

        float[] queryVector = embeddingClient.embed(request.question());

        // Retrieval strategy is swappable: in-Java cosine (default) or pgvector ANN (Neon).
        List<CoverageVectorSearch.ChunkHit> ranked = vectorSearch.search(versionId, queryVector, TOP_K);

        List<String> contexts = ranked.stream().map(CoverageVectorSearch.ChunkHit::chunkText).toList();
        // Record the model that actually answered, not the one configured — the provider may have
        // degraded to its extractive fallback, and the audit row must not claim otherwise.
        ChatClient.Answer generated = chatClient.answer(request.question(), contexts);

        CoverageAnswer answer = new CoverageAnswer();
        answer.setInsuranceProductVersionId(versionId);
        answer.setClaimId(request.claimId());
        answer.setQuestion(request.question());
        answer.setAnswer(generated.text());
        answer.setModel(generated.model());
        answer.setCreatedBy(currentUserId());
        CoverageAnswer savedAnswer = coverageAnswerRepository.save(answer);

        List<AskCoverageResponse.Citation> citations = new ArrayList<>();
        int rank = 1;
        for (CoverageVectorSearch.ChunkHit hit : ranked) {
            CoverageCitation citation = new CoverageCitation();
            citation.setCoverageAnswerId(savedAnswer.getId());
            citation.setPolicyChunkId(hit.chunkId());
            citation.setCitationRank(rank);
            citation.setScore(BigDecimal.valueOf(hit.score()).setScale(6, RoundingMode.HALF_UP));
            citationRepository.save(citation);

            citations.add(new AskCoverageResponse.Citation(
                    hit.chunkId(), hit.chunkIndex(), snippet(hit.chunkText()), round(hit.score())));
            rank++;
        }

        return new AskCoverageResponse(generated.text(), generated.model(), versionId, citations);
    }

    /**
     * The product versions with ingested wording, for the staff assistant's product picker. Only
     * versions the AI can actually answer about appear; each resolves to its product name/code and
     * version number. @TenantId keeps the chunk scan to this tenant.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated() and !hasAuthority('PORTAL_CLAIM_READ')")
    public List<AskableProductResponse> askableProducts() {
        return policyChunkRepository.findDistinctVersionIdsWithKnowledge().stream()
                .map(versionId -> productVersionRepository.findByIdAndIsDeletedFalse(versionId).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(version -> {
                    var product = productRepository
                            .findByIdAndIsDeletedFalse(version.getInsuranceProductId()).orElse(null);
                    if (product == null) {
                        return null;
                    }
                    return new AskableProductResponse(
                            product.getId(), product.getName(), product.getCode(),
                            version.getId(), version.getVersionNumber());
                })
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(AskableProductResponse::productName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Long resolveVersionId(AskCoverageRequest request) {
        if (request.claimId() != null) {
            var claim = claimRepository.findByIdAndIsDeletedFalse(request.claimId())
                    .orElseThrow(() -> new NotFoundException("CLAIM_NOT_FOUND", "Claim not found"));
            if (claim.getInsuranceProductVersionId() == null) {
                throw new BusinessException("CLAIM_HAS_NO_VERSION", "Claim has no pinned product version");
            }
            return claim.getInsuranceProductVersionId();
        }
        if (request.insuranceProductVersionId() != null) {
            return request.insuranceProductVersionId();
        }
        throw new BusinessException("VERSION_REQUIRED", "Provide a claimId or insuranceProductVersionId");
    }

    private static String snippet(String text) {
        String clean = text.strip().replaceAll("\\s+", " ");
        return clean.length() > SNIPPET_CHARS ? clean.substring(0, SNIPPET_CHARS) + "…" : clean;
    }

    private static Double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof ClaimLensPrincipal p ? p.userId() : null;
    }
}
