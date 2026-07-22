import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // next-mdx-remote ships untranspiled ESM; let Next compile it.
  transpilePackages: ["next-mdx-remote"],
};

export default nextConfig;
