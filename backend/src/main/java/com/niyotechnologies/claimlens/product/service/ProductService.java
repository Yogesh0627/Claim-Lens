package com.niyotechnologies.claimlens.product.service;

import com.niyotechnologies.claimlens.product.dto.request.CreateProductRequest;
import com.niyotechnologies.claimlens.product.dto.request.CreateProductVersionRequest;
import com.niyotechnologies.claimlens.product.dto.request.UpdateProductRequest;
import com.niyotechnologies.claimlens.product.dto.response.ProductResponse;
import com.niyotechnologies.claimlens.product.dto.response.ProductVersionResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(Long productId, UpdateProductRequest request);

    ProductResponse getProduct(Long productId);

    List<ProductResponse> getProducts();

    ProductVersionResponse createVersion(Long productId, CreateProductVersionRequest request);

    ProductVersionResponse activateVersion(Long productId, Long versionId);

    List<ProductVersionResponse> getVersions(Long productId);
}
