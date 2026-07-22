import {
  Bell,
  Building2,
  FileText,
  LayoutDashboard,
  Package,
  ScrollText,
  ShieldAlert,
  Users,
  Briefcase,
  IdCard,
  type LucideIcon,
} from "lucide-react";
import { PERMISSIONS } from "./permissions";

export interface NavItem {
  title: string;
  href: string;
  icon: LucideIcon;
  /** Any of these permissions grants access. Omitted = always visible. */
  permission?: string[];
}

export interface NavSection {
  title: string;
  items: NavItem[];
}

export const NAV_SECTIONS: NavSection[] = [
  {
    title: "Overview",
    items: [
      {
        title: "Dashboard",
        href: "/dashboard",
        icon: LayoutDashboard,
        permission: [PERMISSIONS.ANALYTICS_READ],
      },
      { title: "Notifications", href: "/notifications", icon: Bell },
    ],
  },
  {
    title: "Claims",
    items: [
      {
        title: "Claims",
        href: "/claims",
        icon: FileText,
        permission: [PERMISSIONS.CLAIM_READ],
      },
    ],
  },
  {
    title: "Records",
    items: [
      {
        title: "Customers",
        href: "/customers",
        icon: Users,
        permission: [PERMISSIONS.CUSTOMER_READ],
      },
      {
        title: "Policies",
        href: "/policies",
        icon: ScrollText,
        permission: [PERMISSIONS.POLICY_READ],
      },
      {
        title: "Products",
        href: "/products",
        icon: Package,
        permission: [PERMISSIONS.PRODUCT_READ],
      },
    ],
  },
  {
    title: "Configuration",
    items: [
      {
        title: "Fraud Rulesets",
        href: "/rulesets",
        icon: ShieldAlert,
        permission: [PERMISSIONS.RULESET_READ],
      },
    ],
  },
  {
    title: "Organization",
    items: [
      {
        title: "Users",
        href: "/organization/users",
        icon: Users,
        permission: [PERMISSIONS.USER_READ],
      },
      {
        title: "Regions & Branches",
        href: "/organization/regions",
        icon: Building2,
        permission: [PERMISSIONS.ORG_REGION_READ],
      },
      {
        title: "Departments",
        href: "/organization/departments",
        icon: Briefcase,
        permission: [PERMISSIONS.ORG_DEPARTMENT_READ],
      },
      {
        title: "Designations",
        href: "/organization/designations",
        icon: IdCard,
        permission: [PERMISSIONS.ORG_DESIGNATION_READ],
      },
      {
        title: "Companies",
        href: "/organization/companies",
        icon: Building2,
        permission: [PERMISSIONS.ORG_COMPANY_READ],
      },
    ],
  },
];

/** The landing route for a user, based on what they can actually see. */
export function homePathFor(permissions: Set<string>): string {
  // Platform admins live in the cross-tenant console, not a tenant workspace.
  if (permissions.has(PERMISSIONS.PLATFORM_ADMIN)) return "/platform";
  // Customers get the self-service portal, never the staff workspace.
  if (permissions.has(PERMISSIONS.PORTAL_CLAIM_READ)) return "/portal";
  const has = (p?: string[]) => !p || p.some((x) => permissions.has(x));
  for (const section of NAV_SECTIONS) {
    for (const item of section.items) {
      if (item.href === "/notifications") continue;
      if (has(item.permission)) return item.href;
    }
  }
  return "/notifications";
}
