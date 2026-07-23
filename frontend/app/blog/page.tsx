import type { Metadata } from "next";
import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import { getAllPosts } from "@/lib/blog";
import { formatDate } from "@/lib/dayjs";

export const metadata: Metadata = {
  // No brand suffix here — the root layout's title template appends " · ClaimLens".
  title: "Blog",
  description:
    "Notes on motor-insurance claims, fraud detection, and how ClaimLens automates the busywork of claims processing.",
};

export default async function BlogIndexPage() {
  const posts = await getAllPosts();

  return (
    <div className="mx-auto w-full max-w-5xl px-4 py-12 sm:px-6 sm:py-16">
      <div className="max-w-2xl">
        <p className="text-primary text-sm font-medium">The ClaimLens Blog</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
          Claims, fraud &amp; the software that speeds them up
        </h1>
        <p className="text-muted-foreground mt-3 text-base sm:text-lg">
          Plain-language pieces on how motor-insurance claims really work, where fraud hides, and how
          a modern platform reads documents, scores risk, and keeps every decision accountable.
        </p>
      </div>

      {posts.length === 0 ? (
        <p className="text-muted-foreground mt-12">No posts yet — check back soon.</p>
      ) : (
        <div className="mt-10 grid gap-5 sm:mt-12 sm:grid-cols-2">
          {posts.map((post) => (
            <Card key={post.slug} className="group flex flex-col p-0 transition-shadow hover:shadow-md">
              <Link
                href={`/blog/${post.slug}`}
                className="flex h-full flex-col gap-3 p-5 sm:p-6"
                aria-label={`Read ${post.title}`}
              >
                <div className="text-muted-foreground flex items-center gap-2 text-xs">
                  {post.category ? (
                    <span className="bg-primary/10 text-primary rounded-full px-2 py-0.5 font-medium">
                      {post.category}
                    </span>
                  ) : null}
                  <time dateTime={post.date}>{formatDate(post.date)}</time>
                  <span aria-hidden>·</span>
                  <span>{post.readingTime} min read</span>
                </div>
                <h2 className="text-lg font-semibold tracking-tight sm:text-xl">{post.title}</h2>
                <p className="text-muted-foreground flex-1 text-sm">{post.description}</p>
                <span className="text-primary mt-1 inline-flex items-center gap-1 text-sm font-medium">
                  Read more
                  <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
                </span>
              </Link>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
