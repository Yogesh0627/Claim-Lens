package com.niyotechnologies.claimlens.product.service.impl;

import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.product.dto.request.CreateProductRequest;
import com.niyotechnologies.claimlens.product.dto.request.CreateProductVersionRequest;
import com.niyotechnologies.claimlens.product.dto.request.UpdateProductRequest;
import com.niyotechnologies.claimlens.product.dto.response.ProductResponse;
import com.niyotechnologies.claimlens.product.dto.response.ProductVersionResponse;
import com.niyotechnologies.claimlens.product.entity.ClaimType;
import com.niyotechnologies.claimlens.product.entity.InsuranceProduct;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.enums.ProductStatus;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.product.mapper.ProductMapper;
import com.niyotechnologies.claimlens.product.repository.ClaimTypeRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductVersionRepository;
import com.niyotechnologies.claimlens.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tenant scoping is enforced by Hibernate @TenantId; this service never takes a tenant argument. */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    @Autowired
    private final InsuranceProductRepository productRepository;
    @Autowired
    private final InsuranceProductVersionRepository versionRepository;
    @Autowired
    private final ClaimTypeRepository claimTypeRepository;
    @Autowired
    private final ProductMapper productMapper;

    private InsuranceProduct getProductOrThrow(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsByCodeAndIsDeletedFalse(request.code())) {
            throw new BusinessException("PRODUCT_CODE_ALREADY_EXISTS", "Product code already exists");
        }
        ClaimType claimType = claimTypeRepository.findByCode(request.claimTypeCode())
                .orElseThrow(() -> new NotFoundException("CLAIM_TYPE_NOT_FOUND", "Claim type not found"));
        InsuranceProduct product = productMapper.toEntity(request, claimType.getId());
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        InsuranceProduct product = getProductOrThrow(productId);
        product.setName(request.name());
        product.setDescription(request.description());
        try {
            product.setStatus(ProductStatus.valueOf(request.status().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Unknown product status: " + request.status());
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ProductResponse getProduct(Long productId) {
        return productMapper.toResponse(getProductOrThrow(productId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public List<ProductResponse> getProducts() {
        return productMapper.toResponseList(productRepository.findAllByIsDeletedFalse());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ProductVersionResponse createVersion(Long productId, CreateProductVersionRequest request) {
        getProductOrThrow(productId);
        int nextVersionNumber = versionRepository
                .findTopByInsuranceProductIdAndIsDeletedFalseOrderByVersionNumberDesc(productId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);
        InsuranceProductVersion version = new InsuranceProductVersion();
        // tenant_id is populated by Hibernate @TenantId on persist — not set here.
        version.setInsuranceProductId(productId);
        version.setVersionNumber(nextVersionNumber);
        version.setStatus(ProductVersionStatus.DRAFT);
        version.setEffectiveFrom(request.effectiveFrom());
        version.setEffectiveTo(request.effectiveTo());
        version.setCoverageSummary(request.coverageSummary());
        return productMapper.toVersionResponse(versionRepository.save(version));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ProductVersionResponse activateVersion(Long productId, Long versionId) {
        InsuranceProductVersion version = versionRepository.findByIdAndIsDeletedFalse(versionId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_VERSION_NOT_FOUND",
                        "Product version not found"));
        if (!version.getInsuranceProductId().equals(productId)) {
            throw new NotFoundException("PRODUCT_VERSION_NOT_FOUND", "Product version not found");
        }
        // Retire the currently-active version first (and flush) so the one-active-version
        // unique index is not violated when we activate the target version.
        versionRepository
                .findByInsuranceProductIdAndStatusAndIsDeletedFalse(productId, ProductVersionStatus.ACTIVE)
                .ifPresent(active -> {
                    active.setStatus(ProductVersionStatus.RETIRED);
                    versionRepository.saveAndFlush(active);
                });
        version.setStatus(ProductVersionStatus.ACTIVE);
        return productMapper.toVersionResponse(versionRepository.save(version));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public List<ProductVersionResponse> getVersions(Long productId) {
        return productMapper.toVersionResponseList(
                versionRepository.findAllByInsuranceProductIdAndIsDeletedFalse(productId));
    }
}
