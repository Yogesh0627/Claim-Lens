package com.niyotechnologies.claimlens.product.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.product.dto.request.CreateProductRequest;
import com.niyotechnologies.claimlens.product.dto.request.CreateProductVersionRequest;
import com.niyotechnologies.claimlens.product.dto.request.UpdateProductRequest;
import com.niyotechnologies.claimlens.product.dto.response.ProductResponse;
import com.niyotechnologies.claimlens.product.dto.response.ProductVersionResponse;
import com.niyotechnologies.claimlens.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${claimlens.api.base-path}/products")
@RequiredArgsConstructor
public class ProductController {

    @Autowired
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success(productService.createProduct(request));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(productService.updateProduct(productId, request));
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts() {
        return ApiResponse.success(productService.getProducts());
    }

    @PostMapping("/{productId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductVersionResponse> createVersion(
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductVersionRequest request) {
        return ApiResponse.success(productService.createVersion(productId, request));
    }

    @GetMapping("/{productId}/versions")
    public ApiResponse<List<ProductVersionResponse>> getVersions(@PathVariable Long productId) {
        return ApiResponse.success(productService.getVersions(productId));
    }

    @PostMapping("/{productId}/versions/{versionId}/activate")
    public ApiResponse<ProductVersionResponse> activateVersion(
            @PathVariable Long productId,
            @PathVariable Long versionId) {
        return ApiResponse.success(productService.activateVersion(productId, versionId));
    }
}
