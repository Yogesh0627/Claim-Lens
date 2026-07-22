import { http } from "@/lib/http";
import type {
  ApiResponse,
  BranchResponse,
  CreateBranchRequest,
  CreateDepartmentRequest,
  CreateDesignationRequest,
  CreateRegionRequest,
  DepartmentResponse,
  DesignationResponse,
  InsuranceCompanyResponse,
  RegionResponse,
  UpdateBranchRequest,
  UpdateDepartmentRequest,
  UpdateDesignationRequest,
  UpdateRegionRequest,
} from "@/lib/types";

const unwrap = <T>(p: Promise<{ data: ApiResponse<T> }>) => p.then((r) => r.data.data);

export const organizationService = {
  // regions
  listRegions: () => unwrap(http.get<ApiResponse<RegionResponse[]>>("/organizations/regions")),
  getRegion: (id: number) =>
    unwrap(http.get<ApiResponse<RegionResponse>>(`/organizations/regions/${id}`)),
  createRegion: (body: CreateRegionRequest) =>
    unwrap(http.post<ApiResponse<RegionResponse>>("/organizations/regions", body)),
  updateRegion: (id: number, body: UpdateRegionRequest) =>
    unwrap(http.put<ApiResponse<RegionResponse>>(`/organizations/regions/${id}`, body)),
  deleteRegion: (id: number) => http.delete(`/organizations/regions/${id}`).then(() => undefined),

  // branches (nested under a region)
  listBranches: (regionId: number) =>
    unwrap(http.get<ApiResponse<BranchResponse[]>>(`/organizations/regions/${regionId}/branches`)),
  createBranch: (regionId: number, body: CreateBranchRequest) =>
    unwrap(
      http.post<ApiResponse<BranchResponse>>(`/organizations/regions/${regionId}/branches`, body),
    ),
  updateBranch: (regionId: number, branchId: number, body: UpdateBranchRequest) =>
    unwrap(
      http.put<ApiResponse<BranchResponse>>(
        `/organizations/regions/${regionId}/branches/${branchId}`,
        body,
      ),
    ),
  deleteBranch: (regionId: number, branchId: number) =>
    http.delete(`/organizations/regions/${regionId}/branches/${branchId}`).then(() => undefined),

  // departments
  listDepartments: () =>
    unwrap(http.get<ApiResponse<DepartmentResponse[]>>("/organizations/departments")),
  createDepartment: (body: CreateDepartmentRequest) =>
    unwrap(http.post<ApiResponse<DepartmentResponse>>("/organizations/departments", body)),
  updateDepartment: (id: number, body: UpdateDepartmentRequest) =>
    unwrap(http.put<ApiResponse<DepartmentResponse>>(`/organizations/departments/${id}`, body)),
  deleteDepartment: (id: number) =>
    http.delete(`/organizations/departments/${id}`).then(() => undefined),

  // designations
  listDesignations: () =>
    unwrap(http.get<ApiResponse<DesignationResponse[]>>("/organizations/designations")),
  createDesignation: (body: CreateDesignationRequest) =>
    unwrap(http.post<ApiResponse<DesignationResponse>>("/organizations/designations", body)),
  updateDesignation: (id: number, body: UpdateDesignationRequest) =>
    unwrap(http.put<ApiResponse<DesignationResponse>>(`/organizations/designations/${id}`, body)),
  deleteDesignation: (id: number) =>
    http.delete(`/organizations/designations/${id}`).then(() => undefined),

  // companies
  listCompanies: () =>
    unwrap(http.get<ApiResponse<InsuranceCompanyResponse[]>>("/organizations/companies")),
  getCompany: (id: number) =>
    unwrap(http.get<ApiResponse<InsuranceCompanyResponse>>(`/organizations/companies/${id}`)),
};
