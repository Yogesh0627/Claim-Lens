"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ShieldCheck } from "lucide-react";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { useAuth } from "@/hooks/useAuth";
import { NAV_SECTIONS } from "@/lib/navigation";

export function AppSidebar() {
  const pathname = usePathname();
  const { hasAny } = useAuth();

  const visibleSections = NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => !item.permission || hasAny(item.permission)),
  })).filter((section) => section.items.length > 0);

  return (
    <Sidebar>
      <SidebarHeader>
        <div className="flex items-center gap-2 px-2 py-1.5">
          <div className="bg-primary text-primary-foreground flex h-8 w-8 items-center justify-center rounded-lg">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <span className="text-base font-semibold">ClaimLens</span>
        </div>
      </SidebarHeader>
      <SidebarContent>
        {visibleSections.map((section) => (
          <SidebarGroup key={section.title}>
            <SidebarGroupLabel>{section.title}</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {section.items.map((item) => {
                  const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
                  return (
                    <SidebarMenuItem key={item.href}>
                      <SidebarMenuButton asChild isActive={active} tooltip={item.title}>
                        <Link href={item.href}>
                          <item.icon />
                          <span>{item.title}</span>
                        </Link>
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  );
                })}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        ))}
      </SidebarContent>
    </Sidebar>
  );
}
