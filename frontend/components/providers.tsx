"use client";

import { useEffect } from "react";
import { Provider } from "react-redux";
import { ThemeProvider } from "next-themes";
import { store } from "@/store";
import { useAppDispatch } from "@/hooks/redux";
import { bootstrapAuth } from "@/store/authSlice";
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";

function AuthBootstrap({ children }: { children: React.ReactNode }) {
  const dispatch = useAppDispatch();
  useEffect(() => {
    dispatch(bootstrapAuth());
  }, [dispatch]);
  return <>{children}</>;
}

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <Provider store={store}>
      <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
        <TooltipProvider delayDuration={200}>
          <AuthBootstrap>{children}</AuthBootstrap>
          <Toaster richColors position="top-right" />
        </TooltipProvider>
      </ThemeProvider>
    </Provider>
  );
}
