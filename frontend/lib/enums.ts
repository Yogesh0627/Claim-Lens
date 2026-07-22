/**
 * Enum values the UI renders, with display labels and a colour "intent" for status chips.
 * Values match the backend enums exactly.
 */

export type Intent = "neutral" | "info" | "progress" | "success" | "warning" | "danger";

export interface StatusMeta {
  label: string;
  intent: Intent;
}

function titleCase(value: string): string {
  return value
    .toLowerCase()
    .split(/[_\s]+/)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

/** Fallback meta for any status not explicitly mapped. */
export function statusMeta(map: Record<string, StatusMeta>, value?: string | null): StatusMeta {
  if (!value) return { label: "—", intent: "neutral" };
  return map[value] ?? { label: titleCase(value), intent: "neutral" };
}

export const CLAIM_STATUS_META: Record<string, StatusMeta> = {
  DRAFT: { label: "Draft", intent: "neutral" },
  SUBMITTED: { label: "Submitted", intent: "info" },
  AWAITING_ANALYSIS: { label: "Awaiting Analysis", intent: "progress" },
  AWAITING_ASSIGNMENT: { label: "Awaiting Assignment", intent: "progress" },
  AWAITING_ACCEPTANCE: { label: "Awaiting Acceptance", intent: "progress" },
  UNDER_INVESTIGATION: { label: "Under Investigation", intent: "info" },
  WAITING_FOR_CUSTOMER: { label: "Waiting For Customer", intent: "warning" },
  APPROVED: { label: "Approved", intent: "success" },
  REJECTED: { label: "Rejected", intent: "danger" },
  CLOSED: { label: "Closed", intent: "neutral" },
  REOPENED: { label: "Reopened", intent: "warning" },
};

export const RISK_META: Record<string, StatusMeta> = {
  LOW: { label: "Low", intent: "success" },
  MEDIUM: { label: "Medium", intent: "warning" },
  HIGH: { label: "High", intent: "danger" },
};

export const PROCESSING_STATUS_META: Record<string, StatusMeta> = {
  NOT_STARTED: { label: "Not started", intent: "neutral" },
  PENDING: { label: "Pending", intent: "progress" },
  PROCESSING: { label: "Processing", intent: "progress" },
  QUEUED: { label: "Queued", intent: "progress" },
  PARTIAL: { label: "Partial", intent: "warning" },
  COMPLETE: { label: "Complete", intent: "success" },
  FAILED: { label: "Failed", intent: "danger" },
};

export const EXIF_STATE_META: Record<string, StatusMeta> = {
  // UNKNOWN is neutral, never suspicious (social apps strip EXIF).
  UNKNOWN: { label: "EXIF unknown", intent: "neutral" },
  CONSISTENT: { label: "EXIF consistent", intent: "success" },
  INCONSISTENT: { label: "EXIF inconsistent", intent: "danger" },
};

export const GENERIC_STATUS_META: Record<string, StatusMeta> = {
  ACTIVE: { label: "Active", intent: "success" },
  INACTIVE: { label: "Inactive", intent: "neutral" },
  DRAFT: { label: "Draft", intent: "neutral" },
  RETIRED: { label: "Retired", intent: "danger" },
  ONBOARDING: { label: "Onboarding", intent: "info" },
  SUSPENDED: { label: "Suspended", intent: "danger" },
  INVITED: { label: "Invited", intent: "info" },
  TERMINATED: { label: "Terminated", intent: "danger" },
  LAPSED: { label: "Lapsed", intent: "warning" },
  CANCELLED: { label: "Cancelled", intent: "danger" },
  EXPIRED: { label: "Expired", intent: "warning" },
  REMOVED: { label: "Removed", intent: "danger" },
  UPLOADED: { label: "Uploaded", intent: "success" },
  INVALID: { label: "Invalid", intent: "danger" },
  ARCHIVED: { label: "Archived", intent: "neutral" },
};

// ---- select option lists ----
export const REGION_STATUSES = ["ACTIVE", "INACTIVE"] as const;
export const BRANCH_STATUSES = ["ACTIVE", "INACTIVE"] as const;
export const CUSTOMER_STATUSES = ["ACTIVE", "INACTIVE"] as const;
export const SUBSCRIPTION_PLANS = ["BASIC", "ENTERPRISE", "CUSTOM"] as const;

export const NOTE_TYPES = [
  "FRAUD_OBSERVATION",
  "SITE_VISIT",
  "CUSTOMER_INTERACTION",
  "MANAGER_REVIEW",
  "ESCALATION",
  "GENERAL",
] as const;
export const NOTE_SEVERITIES = ["LOW", "MEDIUM", "HIGH"] as const;

export const NOTE_TYPE_META: Record<string, StatusMeta> = {
  FRAUD_OBSERVATION: { label: "Fraud Observation", intent: "danger" },
  SITE_VISIT: { label: "Site Visit", intent: "info" },
  CUSTOMER_INTERACTION: { label: "Customer Interaction", intent: "info" },
  MANAGER_REVIEW: { label: "Manager Review", intent: "progress" },
  ESCALATION: { label: "Escalation", intent: "warning" },
  GENERAL: { label: "General", intent: "neutral" },
};

/** Common document types for the upload picker (free-form string on the backend). */
export const DOCUMENT_TYPES = [
  "RC_BOOK",
  "DRIVING_LICENSE",
  "FIR",
  "REPAIR_ESTIMATE",
  "INVOICE",
  "DAMAGE_PHOTO",
  "POLICY_DOCUMENT",
  "OTHER",
] as const;

export const optionLabel = (value: string): string => titleCase(value);
