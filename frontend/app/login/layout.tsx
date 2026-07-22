import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Sign in",
  description: "Sign in to your ClaimLens workspace to manage motor-insurance claims.",
  robots: { index: true, follow: true },
};

export default function LoginLayout({ children }: { children: React.ReactNode }) {
  return children;
}
