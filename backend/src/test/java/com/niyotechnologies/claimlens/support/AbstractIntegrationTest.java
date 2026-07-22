package com.niyotechnologies.claimlens.support;

import com.niyotechnologies.claimlens.organization.entity.InsuranceCompany;
import com.niyotechnologies.claimlens.organization.entity.Region;
import com.niyotechnologies.claimlens.organization.enums.InsuranceCompanyStatus;
import com.niyotechnologies.claimlens.organization.enums.RegionStatus;
import com.niyotechnologies.claimlens.organization.enums.SubscriptionPlan;
import com.niyotechnologies.claimlens.customer.entity.Customer;
import com.niyotechnologies.claimlens.customer.enums.CustomerStatus;
import com.niyotechnologies.claimlens.customer.repository.CustomerRepository;
import com.niyotechnologies.claimlens.organization.repository.InsuranceCompanyRepository;
import com.niyotechnologies.claimlens.organization.repository.RegionRepository;
import com.niyotechnologies.claimlens.product.entity.InsuranceProduct;
import com.niyotechnologies.claimlens.product.entity.InsuranceProductVersion;
import com.niyotechnologies.claimlens.product.enums.ProductStatus;
import com.niyotechnologies.claimlens.product.enums.ProductVersionStatus;
import com.niyotechnologies.claimlens.processing.worker.AnalysisWorker;
import com.niyotechnologies.claimlens.processing.worker.FraudWorker;
import com.niyotechnologies.claimlens.processing.worker.OcrWorker;
import com.niyotechnologies.claimlens.product.repository.ClaimTypeRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductRepository;
import com.niyotechnologies.claimlens.product.repository.InsuranceProductVersionRepository;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.security.service.JwtService;
import com.niyotechnologies.claimlens.tenancy.context.TenantContext;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.enums.AuthProvider;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * Base for integration tests. Loads the full context against a real Postgres (the `test` profile
 * points at claimlens_test) and drives the app through MockMvc, which runs the complete Spring
 * Security filter chain (JwtAuthenticationFilter -> TenantFilter).
 *
 * Seeding uses the JPA repositories, not the HTTP/service layer under test. For tenant-scoped
 * entities we bind the tenant with {@link #inTenant} BEFORE calling save(), so Hibernate's @TenantId
 * resolver picks it up when the save's session opens (setting it mid-transaction would not work — see
 * the auth war story). This still keeps the fixture independent of the controllers/services being
 * tested: the isolation test proves the *reader* is filtered.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected InsuranceCompanyRepository insuranceCompanyRepository;

    @Autowired
    protected RegionRepository regionRepository;

    @Autowired
    protected AppUserRepository appUserRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected CustomerRepository customerRepository;

    @Autowired
    protected InsuranceProductRepository productRepository;

    @Autowired
    protected InsuranceProductVersionRepository productVersionRepository;

    @Autowired
    protected ClaimTypeRepository claimTypeRepository;

    @Autowired
    protected OcrWorker ocrWorker;

    @Autowired
    protected AnalysisWorker analysisWorker;

    @Autowired
    protected FraudWorker fraudWorker;

    /** Drives the async pipeline synchronously: OCR -> analysis (fires the fraud gate) -> fraud. */
    protected void drivePipeline() {
        ocrWorker.pollOnce();
        analysisWorker.pollOnce();
        fraudWorker.pollOnce();
    }

    @BeforeEach
    void resetBusinessData() throws SQLException {
        // Teardown (not seeding): TRUNCATE via JDBC resets rows + identity sequences in one shot,
        // while preserving the global role/permission seed from V3/V5.
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(
                    "TRUNCATE TABLE coverage_answer_citation, coverage_answer, policy_chunk, "
                            + "audit_log, notification, investigation_note, fraud_rule, fraud_ruleset, "
                            + "fraud_score, fraud_job, analysis_job, analysis_result, ocr_job, ocr_result, "
                            + "claim_processing_state, document, "
                            + "claim_assignment, claim_status_history, "
                            + "claim, insured_vehicle, insurance_policy, insurance_product_version, "
                            + "insurance_product, user_session, user_branch_assignment, app_user, customer, "
                            + "branch, region, department, designation, insurance_company "
                            + "RESTART IDENTITY CASCADE");
        }
    }

    /** Runs an action with the given tenant bound, so @TenantId stamps saves for that tenant. */
    protected <T> T inTenant(long tenantId, Supplier<T> action) {
        try {
            TenantContext.set(tenantId);
            return action.get();
        } finally {
            TenantContext.clear();
        }
    }

    protected String tokenFor(long userId, long tenantId, long roleId) {
        return jwtService.generateToken(new ClaimLensPrincipal(
                userId, tenantId, roleId,
                "u" + userId + "@t" + tenantId + ".test", "EMP" + userId, null));
    }

    protected String customerTokenFor(long userId, long tenantId, long roleId, long customerId) {
        return jwtService.generateToken(new ClaimLensPrincipal(
                userId, tenantId, roleId,
                "u" + userId + "@t" + tenantId + ".test", "EMP" + userId, customerId));
    }

    protected long insertCompany(String name, String code, String tenantKey) {
        // InsuranceCompany is the tenant root (no @TenantId) — no tenant binding needed.
        InsuranceCompany company = new InsuranceCompany();
        company.setName(name);
        company.setCode(code);
        company.setTenantKey(tenantKey);
        company.setStatus(InsuranceCompanyStatus.ONBOARDING);
        company.setSubscriptionPlan(SubscriptionPlan.BASIC);
        company.setCurrency("INR");
        company.setTimezone("Asia/Kolkata");
        return insuranceCompanyRepository.saveAndFlush(company).getId();
    }

    protected long insertRegion(long tenantId, String code, String name) {
        return inTenant(tenantId, () -> {
            Region region = new Region();
            region.setCode(code);
            region.setName(name);
            region.setStatus(RegionStatus.ACTIVE);
            return regionRepository.saveAndFlush(region).getId();
        });
    }

    protected long insertUser(long tenantId, String email, String passwordHash, long roleId) {
        return inTenant(tenantId, () -> {
            AppUser user = new AppUser();
            user.setEmployeeCode("EMP-" + email);
            user.setFirstName("Test");
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setRoleId(roleId);
            user.setStatus(UserStatus.ACTIVE);
            user.setAuthProvider(AuthProvider.LOCAL);
            return appUserRepository.saveAndFlush(user).getId();
        });
    }

    protected long roleIdByCode(String code) {
        return roleRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new IllegalStateException("Seed role not found: " + code))
                .getId();
    }

    protected long claimTypeIdByCode(String code) {
        return claimTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Seed claim type not found: " + code))
                .getId();
    }

    protected long insertCustomer(long tenantId, String customerNumber) {
        return inTenant(tenantId, () -> {
            Customer customer = new Customer();
            customer.setCustomerNumber(customerNumber);
            customer.setFirstName("Test");
            customer.setLastName("Customer");
            customer.setStatus(CustomerStatus.ACTIVE);
            return customerRepository.saveAndFlush(customer).getId();
        });
    }

    protected long insertProduct(long tenantId, String code) {
        long claimTypeId = claimTypeIdByCode("MOTOR");
        return inTenant(tenantId, () -> {
            InsuranceProduct product = new InsuranceProduct();
            product.setClaimTypeId(claimTypeId);
            product.setCode(code);
            product.setName(code + " product");
            product.setStatus(ProductStatus.ACTIVE);
            return productRepository.saveAndFlush(product).getId();
        });
    }

    protected long insertProductVersion(long tenantId, long productId, int versionNumber,
                                        ProductVersionStatus status) {
        return inTenant(tenantId, () -> {
            InsuranceProductVersion version = new InsuranceProductVersion();
            version.setInsuranceProductId(productId);
            version.setVersionNumber(versionNumber);
            version.setStatus(status);
            version.setEffectiveFrom(java.time.LocalDate.of(2024, 1, 1));
            return productVersionRepository.saveAndFlush(version).getId();
        });
    }
}
