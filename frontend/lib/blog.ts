import { promises as fs } from "fs";
import path from "path";
import { compileMDX } from "next-mdx-remote/rsc";

const BLOG_DIR = path.join(process.cwd(), "content", "blog");

export interface BlogFrontmatter {
  title: string;
  description: string;
  date: string;
  category?: string;
  author?: string;
  tags?: string[];
}

export interface BlogMeta extends BlogFrontmatter {
  slug: string;
  readingTime: number;
}

async function readRaw(slug: string): Promise<string | null> {
  try {
    return await fs.readFile(path.join(BLOG_DIR, `${slug}.mdx`), "utf-8");
  } catch {
    return null;
  }
}

/** Whole-body word count / 200 wpm, frontmatter stripped. */
function readingTimeOf(raw: string): number {
  const body = raw.replace(/^---[\s\S]*?---/, "");
  const words = body.trim().split(/\s+/).filter(Boolean).length;
  return Math.max(1, Math.round(words / 200));
}

/** Compiled MDX (React node) + frontmatter + reading time for one post, or null if missing. */
export async function getPost(slug: string) {
  const raw = await readRaw(slug);
  if (!raw) return null;
  const { content, frontmatter } = await compileMDX<BlogFrontmatter>({
    source: raw,
    options: { parseFrontmatter: true },
  });
  return { slug, content, frontmatter, readingTime: readingTimeOf(raw) };
}

/** Frontmatter for every post, newest first. */
export async function getAllPosts(): Promise<BlogMeta[]> {
  let files: string[] = [];
  try {
    files = await fs.readdir(BLOG_DIR);
  } catch {
    return [];
  }
  const posts = await Promise.all(
    files
      .filter((f) => f.endsWith(".mdx"))
      .map(async (f) => {
        const slug = f.replace(/\.mdx$/, "");
        const raw = (await readRaw(slug)) ?? "";
        const { frontmatter } = await compileMDX<BlogFrontmatter>({
          source: raw,
          options: { parseFrontmatter: true },
        });
        return { slug, readingTime: readingTimeOf(raw), ...frontmatter };
      }),
  );
  return posts.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
}
