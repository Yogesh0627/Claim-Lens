"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LogOut, UserCircle } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ProfileDialog } from "@/components/profile-dialog";
import { useAppDispatch } from "@/hooks/redux";
import { useAuth } from "@/hooks/useAuth";
import { logout } from "@/store/authSlice";
import { optionLabel } from "@/lib/enums";

export function UserMenu() {
  const dispatch = useAppDispatch();
  const router = useRouter();
  const { user } = useAuth();
  const [profileOpen, setProfileOpen] = useState(false);

  const onLogout = async () => {
    await dispatch(logout());
    router.replace("/sign-in");
  };

  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(" ").trim();
  const label = fullName || user?.email || "User";
  const roleLabel = user?.roleName ?? (user?.roleCode ? optionLabel(user.roleCode) : null);
  const avatar = (fullName || user?.email || "U")
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="h-9 gap-2 px-2">
          <Avatar className="h-7 w-7">
            <AvatarFallback className="text-xs">{avatar}</AvatarFallback>
          </Avatar>
          <span className="hidden max-w-44 flex-col items-start leading-tight sm:flex">
            <span className="truncate text-sm">{label}</span>
            {roleLabel ? (
              <span className="text-muted-foreground truncate text-xs">{roleLabel}</span>
            ) : null}
          </span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel className="flex flex-col">
          <span className="truncate">{label}</span>
          {roleLabel ? (
            <span className="text-muted-foreground text-xs font-normal">{roleLabel}</span>
          ) : null}
          {user?.email && user.email !== label ? (
            <span className="text-muted-foreground truncate text-xs font-normal">{user.email}</span>
          ) : null}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={() => setProfileOpen(true)} className="cursor-pointer">
          <UserCircle className="mr-2 h-4 w-4" /> My profile
        </DropdownMenuItem>
        <DropdownMenuItem onClick={onLogout} className="cursor-pointer">
          <LogOut className="mr-2 h-4 w-4" /> Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
      <ProfileDialog open={profileOpen} onOpenChange={setProfileOpen} />
    </DropdownMenu>
  );
}
