import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // next-mdx-remote ships untranspiled ESM; let Next compile it.
  transpilePackages: ["next-mdx-remote"],

  async redirects() {
    return [
      // The sign-in page moved from /login to /sign-in. Keep the old path working so existing
      // bookmarks, links already shared, and anything cached by a browser don't hit a 404.
      { source: "/login", destination: "/sign-in", permanent: true },
    ];
  },
};

export default nextConfig;
