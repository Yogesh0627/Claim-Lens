import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { getAllPosts, getPost } from "@/lib/blog";
import { formatDate } from "@/lib/dayjs";

export async function generateStaticParams() {
  const posts = await getAllPosts();
  return posts.map((post) => ({ slug: post.slug }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const post = await getPost(slug);
  // Titles omit the brand: the root layout's template appends " · ClaimLens".
  if (!post) return { title: "Post not found" };
  return {
    title: post.frontmatter.title,
    description: post.frontmatter.description,
    openGraph: {
      title: post.frontmatter.title,
      description: post.frontmatter.description,
      type: "article",
      publishedTime: post.frontmatter.date,
    },
  };
}

export default async function BlogPostPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const post = await getPost(slug);
  if (!post) notFound();

  const { content, frontmatter, readingTime } = post;

  return (
    <article className="mx-auto w-full max-w-3xl px-4 py-10 sm:px-6 sm:py-14">
      <Link
        href="/blog"
        className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
      >
        <ArrowLeft className="h-4 w-4" /> All posts
      </Link>

      <header className="mt-6">
        <div className="text-muted-foreground flex flex-wrap items-center gap-2 text-xs">
          {frontmatter.category ? (
            <span className="bg-primary/10 text-primary rounded-full px-2 py-0.5 font-medium">
              {frontmatter.category}
            </span>
          ) : null}
          <time dateTime={frontmatter.date}>{formatDate(frontmatter.date)}</time>
          <span aria-hidden>·</span>
          <span>{readingTime} min read</span>
          {frontmatter.author ? (
            <>
              <span aria-hidden>·</span>
              <span>{frontmatter.author}</span>
            </>
          ) : null}
        </div>
      </header>

      <div className="prose prose-neutral dark:prose-invert mt-6 max-w-none prose-headings:scroll-mt-20 prose-h1:text-3xl prose-h1:sm:text-4xl">
        {content}
      </div>

      {frontmatter.tags && frontmatter.tags.length > 0 ? (
        <div className="mt-10 flex flex-wrap gap-2 border-t pt-6">
          {frontmatter.tags.map((tag) => (
            <span
              key={tag}
              className="bg-muted text-muted-foreground rounded-full px-2.5 py-0.5 text-xs"
            >
              #{tag}
            </span>
          ))}
        </div>
      ) : null}
    </article>
  );
}
