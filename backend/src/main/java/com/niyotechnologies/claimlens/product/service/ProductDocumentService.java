package com.niyotechnologies.claimlens.product.service;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.product.dto.response.ProductDocumentResponse;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.entity.ProductDocument;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductVersionRepository;
import com.niyotechnologies.claimlens.product.repository.ProductDocumentRepository;
import com.niyotechnologies.claimlens.coverage.dto.IngestKnowledgeResponse;
import com.niyotechnologies.claimlens.coverage.service.CoverageService;
import com.niyotechnologies.claimlens.document.storage.DocumentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Files attached to a product version — chiefly the policy-wording PDF whose terms a policy issued
 * under that version is governed by. Bytes go to object storage; the row holds the pointer. All
 * access is @TenantId-scoped and gated by the product permissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDocumentService {

    private static final String DEFAULT_TYPE = "POLICY_WORDING";
    /** Below this many characters, extracted "text" is treated as noise (e.g. a scanned image PDF). */
    private static final int MIN_INGEST_CHARS = 40;

    @Autowired
    private final ProductDocumentRepository documentRepository;
    @Autowired
    private final InsuranceProductVersionRepository versionRepository;
    @Autowired
    private final DocumentStorage documentStorage;
    @Autowired
    private final CoverageService coverageService;

    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ProductDocumentResponse upload(Long productId, Long versionId, MultipartFile file, String documentType) {
        InsuranceProductVersion version = requireVersion(productId, versionId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "No file was provided");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("UPLOAD_FAILED", "Could not read the uploaded file");
        }

        String storageKey = documentStorage.store(bytes, file.getOriginalFilename());

        ProductDocument doc = new ProductDocument();
        doc.setInsuranceProductId(version.getInsuranceProductId());
        doc.setInsuranceProductVersionId(versionId);
        doc.setDocumentType(documentType == null || documentType.isBlank()
                ? DEFAULT_TYPE : documentType.toUpperCase());
        doc.setFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setSizeBytes(file.getSize());
        doc.setStorageKey(storageKey);
        ProductDocument saved = documentRepository.save(doc);

        Integer indexedSections = autoIngest(version, bytes, file);
        return toResponse(saved, indexedSections);
    }

    /**
     * Best-effort: if the uploaded file is a text-bearing PDF, extract its text and ingest it into the
     * AI knowledge base for this version — so one upload feeds both the human download and the chatbot.
     * Pure side-effect: any failure (not a PDF, a scanned/image PDF with no text, an embedding hiccup)
     * is swallowed so the upload itself never fails. Returns the chunk count, or null if nothing was
     * ingested. Uses the API-key-free local PDFBox extractor and the embedding quota (not the capped
     * chat model), so it costs nothing and can't hit the daily answer limit.
     */
    private Integer autoIngest(InsuranceProductVersion version, byte[] bytes, MultipartFile file) {
        if (!isPdf(file)) {
            return null;
        }
        String text = extractPdfText(bytes);
        if (text == null || text.strip().length() < MIN_INGEST_CHARS) {
            log.info("Skipping auto-ingest of '{}' — no extractable text (likely a scanned image PDF)",
                    file.getOriginalFilename());
            return null;
        }
        try {
            IngestKnowledgeResponse result = coverageService.ingestText(
                    version.getInsuranceProductId(), version.getId(), text);
            log.info("Auto-ingested '{}' into version {} — {} chunks",
                    file.getOriginalFilename(), version.getId(), result.chunkCount());
            return (int) result.chunkCount();
        } catch (Exception e) {
            log.warn("Auto-ingest of '{}' failed (upload still succeeded): {}",
                    file.getOriginalFilename(), e.getMessage());
            return null;
        }
    }

    private static boolean isPdf(MultipartFile file) {
        String type = file.getContentType();
        String name = file.getOriginalFilename();
        return (type != null && type.toLowerCase().contains("pdf"))
                || (name != null && name.toLowerCase().endsWith(".pdf"));
    }

    /** Local, free text extraction via PDFBox. Returns null for a non-PDF or an unreadable file. */
    private static String extractPdfText(byte[] bytes) {
        try (PDDocument document = PDDocument.load(bytes)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public List<ProductDocumentResponse> listForVersion(Long productId, Long versionId) {
        requireVersion(productId, versionId);
        return documentRepository
                .findAllByInsuranceProductVersionIdAndIsDeletedFalseOrderByCreatedAtDesc(versionId)
                .stream().map(ProductDocumentService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public DownloadedFile download(Long documentId) {
        ProductDocument doc = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new NotFoundException("DOCUMENT_NOT_FOUND", "Document not found"));
        byte[] content = documentStorage.retrieve(doc.getStorageKey());
        return new DownloadedFile(doc.getFileName(), doc.getContentType(), content);
    }

    /**
     * Portal path: list a version's documents WITHOUT the staff PRODUCT_READ gate. A customer holds
     * only PORTAL_* permissions, so the caller ({@code PortalService}) must have already proven the
     * customer owns a policy pinned to this version. Still @TenantId-scoped, so it can only ever
     * return documents in the customer's own tenant.
     */
    @Transactional(readOnly = true)
    public List<ProductDocumentResponse> listForVersionInternal(Long versionId) {
        return documentRepository
                .findAllByInsuranceProductVersionIdAndIsDeletedFalseOrderByCreatedAtDesc(versionId)
                .stream().map(ProductDocumentService::toResponse).toList();
    }

    /**
     * Portal path: download a document, verifying it belongs to the expected (customer-owned) version
     * so a customer can't fetch another version's wording by guessing an id. No PRODUCT_READ gate —
     * see {@link #listForVersionInternal}.
     */
    @Transactional(readOnly = true)
    public DownloadedFile downloadForVersionInternal(Long documentId, Long versionId) {
        ProductDocument doc = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new NotFoundException("DOCUMENT_NOT_FOUND", "Document not found"));
        if (!versionId.equals(doc.getInsuranceProductVersionId())) {
            // 404, not 403 — don't confirm a document that isn't on this customer's policy version.
            throw new NotFoundException("DOCUMENT_NOT_FOUND", "Document not found");
        }
        byte[] content = documentStorage.retrieve(doc.getStorageKey());
        return new DownloadedFile(doc.getFileName(), doc.getContentType(), content);
    }

    /** Verifies the version exists in this tenant and actually belongs to the given product. */
    private InsuranceProductVersion requireVersion(Long productId, Long versionId) {
        InsuranceProductVersion version = versionRepository.findByIdAndIsDeletedFalse(versionId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_VERSION_NOT_FOUND", "Product version not found"));
        if (!version.getInsuranceProductId().equals(productId)) {
            // 404, not 403 — don't confirm the version exists under a different product.
            throw new NotFoundException("PRODUCT_VERSION_NOT_FOUND", "Product version not found");
        }
        return version;
    }

    private static ProductDocumentResponse toResponse(ProductDocument d) {
        return toResponse(d, null);
    }

    private static ProductDocumentResponse toResponse(ProductDocument d, Integer indexedSections) {
        return new ProductDocumentResponse(
                d.getId(), d.getDocumentType(), d.getFileName(),
                d.getContentType(), d.getSizeBytes(), d.getCreatedAt(), indexedSections);
    }

    /** The bytes plus enough metadata for the controller to stream the download. */
    public record DownloadedFile(String fileName, String contentType, byte[] content) {
    }
}
