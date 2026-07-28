/**
 * TypeScript mirrors of the backend DTOs. Field names and nullability match the Java records
 * exactly (see the API contract). Instant/LocalDate arrive as ISO strings over JSON.
 */

// ---- envelope ----
export interface ApiResponse<T> {
  success: boolean;
  data: T;
}

/** Page envelope returned by list endpoints — mirrors the backend PagedResponse record. */
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/** Query params every paginated list endpoint accepts. */
export interface PageParams {
  page?: number;
  size?: number;
}

// ---- auth ----
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: number;
  tenantId: number;
  roleId: number;
}

export interface MeResponse {
  userId: number;
  tenantId: number;
  roleId: number;
  roleCode: string | null;
  roleName: string | null;
  firstName: string | null;
  lastName: string | null;
  email: string;
  employeeCode: string;
  customerId: number | null;
  permissions: string[];
}

// ---- users ----
export interface UserResponse {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string | null;
  email: string;
  phone: string | null;
  roleId: number;
  roleCode: string | null;
  status: string;
  departmentId: number | null;
  departmentName: string | null;
  designationId: number | null;
  designationName: string | null;
  regionId: number | null;
  regionName: string | null;
  homeBranchId: number | null;
  homeBranchName: string | null;
  branches: { id: number; name: string; primary: boolean }[];
}
export interface ProfileResponse {
  userId: number;
  firstName: string;
  lastName: string | null;
  email: string;
  phone: string | null;
  employeeCode: string | null;
  roleCode: string | null;
  roleName: string | null;
  departmentName: string | null;
  designationName: string | null;
  regionName: string | null;
  homeBranchName: string | null;
  homeBranchLocation: string | null;
  branchNames: string[];
  customerNumber: string | null;
}
export interface UpdateProfileRequest {
  firstName: string;
  lastName?: string | null;
  phone?: string | null;
}
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface CreateUserRequest {
  email: string;
  firstName: string;
  lastName?: string | null;
  employeeCode: string;
  phone?: string | null;
  roleCode: string;
  /** Omit to invite: the account is created INVITED and emailed a "set your password" link. */
  password?: string | null;
  /** Required for roleCode "CUSTOMER" (links the login to a policyholder); rejected for staff roles. */
  customerId?: number | null;
  departmentId?: number | null;
  designationId?: number | null;
  regionId?: number | null;
  homeBranchId?: number | null;
  branchIds?: number[] | null;
}
export interface UpdateUserRequest {
  firstName: string;
  lastName?: string | null;
  phone?: string | null;
  roleCode: string;
  status: string;
  departmentId?: number | null;
  designationId?: number | null;
  regionId?: number | null;
  homeBranchId?: number | null;
  branchIds?: number[] | null;
}

// ---- roles ----
export interface RoleResponse {
  id: number;
  code: string;
  name: string;
  description: string | null;
  status: string;
  isSystemRole: boolean;
}

// ---- organization ----
export interface RegionResponse {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  ownerUserId: number | null;
  status: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}
export interface CreateRegionRequest {
  code: string;
  name: string;
  ownerUserId?: number | null;
  status: string;
  description?: string | null;
}
export type UpdateRegionRequest = Omit<CreateRegionRequest, "code">;

export interface BranchResponse {
  id: number;
  tenantId: number;
  regionId: number;
  code: string;
  name: string;
  ownerUserId: number | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
  state: string | null;
  country: string | null;
  postalCode: string | null;
  status: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}
export interface CreateBranchRequest {
  code: string;
  name: string;
  ownerUserId?: number | null;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;
  postalCode?: string | null;
  status: string;
  description?: string | null;
}
export type UpdateBranchRequest = Omit<CreateBranchRequest, "code">;

export interface DepartmentResponse {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  description: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}
export interface CreateDepartmentRequest {
  tenantId: number;
  code: string;
  name: string;
  description?: string | null;
}
export interface UpdateDepartmentRequest {
  name: string;
  description?: string | null;
}

export interface DesignationResponse {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  description: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}
export interface CreateDesignationRequest {
  code: string;
  name: string;
  description?: string | null;
}
export interface UpdateDesignationRequest {
  name: string;
  description?: string | null;
}

export interface InsuranceCompanyResponse {
  id: number;
  name: string;
  code: string;
  tenantKey: string;
  status: string;
  subscriptionPlan: string;
  currency: string;
  timezone: string;
  contactEmail?: string | null;
  createdAt: string;
  isDeleted: boolean;
  deletedAt?: string | null;
}

export interface UpdateTenantRequest {
  name: string;
  subscriptionPlan?: string;
  currency: string;
  timezone: string;
  contactEmail?: string | null;
}

// ---- customer ----
export interface CustomerResponse {
  id: number;
  publicId: string;
  customerNumber: string;
  firstName: string;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  dateOfBirth: string | null;
  nationalId: string | null;
  address: string | null;
  city: string | null;
  state: string | null;
  country: string | null;
  postalCode: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}
export interface CreateCustomerRequest {
  customerNumber: string;
  firstName: string;
  lastName?: string | null;
  email?: string | null;
  phone?: string | null;
  dateOfBirth?: string | null;
  nationalId?: string | null;
  address?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;
  postalCode?: string | null;
}
export type UpdateCustomerRequest = Omit<CreateCustomerRequest, "customerNumber"> & {
  status: string;
};

// ---- product ----
export interface ProductResponse {
  id: number;
  code: string;
  name: string;
  description: string | null;
  claimTypeId: number;
  status: string;
  createdAt: string;
}
export interface CreateProductRequest {
  code: string;
  name: string;
  description?: string | null;
  claimTypeCode: string;
}
export interface UpdateProductRequest {
  name: string;
  description?: string | null;
  status: string;
}
export interface ProductVersionResponse {
  id: number;
  insuranceProductId: number;
  versionNumber: number;
  status: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  coverageSummary: string | null;
  createdAt: string;
}
export interface CreateProductVersionRequest {
  effectiveFrom: string;
  effectiveTo?: string | null;
  coverageSummary?: string | null;
}

// ---- policy ----
export interface VehicleResponse {
  id: number;
  registrationNumber: string;
  make: string;
  model: string;
  variant: string | null;
  manufactureYear: number | null;
  chassisNumber: string | null;
  engineNumber: string | null;
  colour: string | null;
  fuelType: string | null;
  seatingCapacity: number | null;
  idv: number | null;
  status: string;
}
export interface VehicleRequest {
  registrationNumber: string;
  make: string;
  model: string;
  variant?: string | null;
  manufactureYear?: number | null;
  chassisNumber?: string | null;
  engineNumber?: string | null;
  colour?: string | null;
  fuelType?: string | null;
  seatingCapacity?: number | null;
  idv?: number | null;
}
export interface ProductDocumentResponse {
  id: number;
  documentType: string;
  fileName: string;
  contentType: string | null;
  sizeBytes: number | null;
  createdAt: string;
  // On the upload response: how many text chunks were auto-extracted and indexed into the AI
  // knowledge base. Null when not applicable (non-PDF, no extractable text, or a plain list row).
  indexedSections?: number | null;
}

export interface PolicyResponse {
  id: number;
  publicId: string;
  policyNumber: string;
  customerId: number;
  customerName: string | null;
  insuranceProductId: number;
  productName: string | null;
  productCode: string | null;
  insuranceProductVersionId: number;
  productVersionNumber: number | null;
  effectiveFrom: string;
  effectiveTo: string;
  sumInsured: number;
  deductible: number | null;
  premiumAmount: number | null;
  currency: string | null;
  status: string;
  issuedAt: string;
  vehicle: VehicleResponse;
}
export interface CreatePolicyRequest {
  policyNumber: string;
  customerId: number;
  insuranceProductId: number;
  effectiveFrom: string;
  effectiveTo: string;
  sumInsured: number;
  deductible?: number | null;
  premiumAmount?: number | null;
  currency?: string | null;
  vehicle: VehicleRequest;
}

// ---- claim ----
export interface ClaimResponse {
  id: number;
  publicId: string;
  claimNumber: string;
  customerId: number;
  customerName: string | null;
  customerNumber: string | null;
  raisedByName: string | null;
  raisedByCode: string | null;
  raisedByStaff: boolean;
  investigatingOfficerName: string | null;
  investigatingOfficerCode: string | null;
  insurancePolicyId: number;
  insuranceProductVersionId: number | null;
  productName: string | null;
  productVersionNumber: number | null;
  policyNumber: string | null;
  vehicleRegistrationNumber: string | null;
  incidentDate: string;
  claimAmount: number | null;
  status: string;
  submittedAt: string | null;
  warnings: string[];
}
export interface ClaimTimelineEntry {
  fromStatus: string | null;
  toStatus: string;
  note: string | null;
  at: string;
}
export interface CreateClaimRequest {
  customerId: number;
  insurancePolicyId: number;
  incidentDate: string;
  claimAmount?: number | null;
  vehicleRegistrationNumber?: string | null;
  description?: string | null;
}
/** Customer portal: no customerId — the server derives it from the authenticated policyholder. */
export interface FileClaimRequest {
  insurancePolicyId: number;
  incidentDate: string;
  claimAmount?: number | null;
  vehicleRegistrationNumber?: string | null;
  description?: string | null;
}
export interface AssignClaimRequest {
  investigatorUserId: number;
}
export interface ClaimDecisionRequest {
  decision: "APPROVE" | "REJECT";
  reason?: string | null;
  /** On REJECT: was the claim actually fraudulent? Feeds the fraud-model evaluation loop. */
  fraudConfirmed?: boolean | null;
}

// ---- document ----
export interface DocumentResponse {
  id: number;
  publicId: string;
  claimId: number;
  documentType: string;
  fileName: string;
  contentType: string | null;
  sizeBytes: number;
  status: string;
  createdAt: string;
}
export interface DocumentVersionResponse {
  id: number;
  documentId: number;
  versionNumber: number;
  fileName: string;
  contentType: string | null;
  sizeBytes: number | null;
  status: string;
  current: boolean;
  createdAt: string;
}

// ---- ruleset / fraud rule ----
export interface FraudRuleResponse {
  id: number;
  code: string;
  description: string | null;
  weight: number;
  enabled: boolean;
}
/** One implemented fraud rule the config UI can offer (from the backend rule registry). */
export interface FraudRuleCatalogEntry {
  code: string;
  description: string;
  defaultWeight: number;
}

/** A role and the permissions it grants (roles-and-permissions view). */
export interface PermissionSummary {
  code: string;
  name: string;
  module: string;
}
export interface RoleWithPermissions {
  id: number;
  code: string;
  name: string;
  description: string | null;
  permissions: PermissionSummary[];
  // True when THIS tenant has customized the role; false = inheriting the platform default.
  customized: boolean;
}
export interface FraudRulesetResponse {
  id: number;
  claimTypeId: number;
  name: string;
  mediumThreshold: number;
  highThreshold: number;
  status: string;
  rules: FraudRuleResponse[];
}
export interface FraudRuleInput {
  code: string;
  description?: string | null;
  weight: number;
  enabled: boolean;
}
export interface CreateFraudRulesetRequest {
  claimTypeCode: string;
  name: string;
  mediumThreshold: number;
  highThreshold: number;
  rules: FraudRuleInput[];
}

// ---- investigation ----
export interface NoteResponse {
  id: number;
  claimId: number;
  noteType: string;
  note: string;
  severity: string | null;
  documentId: number | null;
  createdBy: number;
  createdAt: string;
}
export interface AddNoteRequest {
  noteType: string;
  note: string;
  severity?: string | null;
  documentId?: number | null;
}

// ---- audit / notification / analytics ----
export interface AuditEntryResponse {
  id: number;
  action: string;
  entityType: string;
  entityId: number;
  userId: number;
  createdAt: string;
}
export interface NotificationResponse {
  id: number;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}
export interface DashboardResponse {
  claimsByStatus: Record<string, number>;
  fraudRiskDistribution: Record<string, number>;
}

// ---- processing (OCR + analysis + fraud) ----
export interface OcrResultResponse {
  id: number;
  documentId: number;
  engine: string;
  extractedText: string | null;
  confidence: number | null;
  registrationNumbers: string | null;
  policyNumbers: string | null;
  createdAt: string;
}
export interface AnalysisResultResponse {
  id: number;
  documentId: number;
  phash: string | null;
  exifState: string | null;
  syntheticSignal: boolean;
  syntheticScore: number | null;
  duplicateOfClaimId: number | null;
  createdAt: string;
}
export interface ClaimProcessingResponse {
  state: { ocrStatus: string; analysisStatus: string; fraudStatus: string } | null;
  fraud: { score: number | null; riskLevel: string | null; explanation: string | null } | null;
  ocrResults: OcrResultResponse[];
  analysisResults: AnalysisResultResponse[];
}

// ---- coverage (AI / RAG policy intelligence) ----
export interface IngestKnowledgeResponse {
  insuranceProductVersionId: number;
  chunkCount: number;
  embeddingModel: string | null;
}
export interface CoverageCitation {
  policyChunkId: number;
  chunkIndex: number;
  snippet: string;
  score: number | null;
}
export interface AskCoverageRequest {
  insuranceProductVersionId?: number | null;
  claimId?: number | null;
  question: string;
}
export interface AskCoverageResponse {
  answer: string;
  model: string;
  insuranceProductVersionId: number;
  citations: CoverageCitation[];
}
export interface AskableProduct {
  productId: number;
  productName: string;
  productCode: string;
  versionId: number;
  versionNumber: number;
}

// ---- platform (cross-tenant SaaS admin) ----
export interface PlatformTotals {
  tenants: number;
  activeTenants: number;
  onboardingTenants: number;
  suspendedTenants: number;
  users: number;
  claims: number;
  policies: number;
}
export interface TenantStat {
  tenantId: number;
  name: string;
  code: string;
  status: string;
  subscriptionPlan: string;
  currency: string;
  createdAt: string;
  users: number;
  claims: number;
  highRisk: number;
  mediumRisk: number;
  lowRisk: number;
}
export interface GrowthPoint {
  month: string;
  newTenants: number;
  newClaims: number;
}
export interface PlatformAnalyticsResponse {
  totals: PlatformTotals;
  tenants: TenantStat[];
  growth: GrowthPoint[];
}
export interface OnboardTenantRequest {
  name: string;
  code: string;
  tenantKey: string;
  subscriptionPlan?: string;
  currency: string;
  timezone: string;
  contactEmail?: string | null;
}
export interface ImpersonationResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  tenantId: number;
  tenantName: string;
}
