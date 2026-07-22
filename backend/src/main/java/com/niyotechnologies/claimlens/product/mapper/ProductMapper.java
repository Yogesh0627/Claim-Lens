package com.niyotechnologies.claimlens.product.mapper;

import com.niyotechnologies.claimlens.product.dto.request.CreateProductRequest;
import com.niyotechnologies.claimlens.product.dto.response.ProductResponse;
import com.niyotechnologies.claimlens.product.dto.response.ProductVersionResponse;
import com.niyotechnologies.claimlens.product.entity.InsuranceProduct;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.enums.ProductStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {

    public InsuranceProduct toEntity(CreateProductRequest request, Long claimTypeId) {
        InsuranceProduct product = new InsuranceProduct();
        // tenant_id is populated by Hibernate @TenantId on persist — not set here.
        product.setCode(request.code().trim());
        product.setName(request.name().trim());
        product.setDescription(trim(request.description()));
        product.setClaimTypeId(claimTypeId);
        product.setStatus(ProductStatus.ACTIVE);
        return product;
    }

    public ProductResponse toResponse(InsuranceProduct p) {
        return new ProductResponse(
                p.getId(), p.getCode(), p.getName(), p.getDescription(),
                p.getClaimTypeId(), p.getStatus(), p.getCreatedAt());
    }

    public List<ProductResponse> toResponseList(List<InsuranceProduct> products) {
        return products.stream().map(this::toResponse).toList();
    }

    public ProductVersionResponse toVersionResponse(InsuranceProductVersion v) {
        return new ProductVersionResponse(
                v.getId(), v.getInsuranceProductId(), v.getVersionNumber(), v.getStatus(),
                v.getEffectiveFrom(), v.getEffectiveTo(), v.getCoverageSummary(), v.getCreatedAt());
    }

    public List<ProductVersionResponse> toVersionResponseList(List<InsuranceProductVersion> versions) {
        return versions.stream().map(this::toVersionResponse).toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
