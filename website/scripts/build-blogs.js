import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import matter from 'gray-matter';
import { marked } from 'marked';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const websiteDir = path.resolve(__dirname, '..');
const blogsDir = path.resolve(websiteDir, '..', 'docs', 'blogs');
const imgSrcDir = path.join(blogsDir, 'img');
const imgDestDir = path.join(websiteDir, 'public', 'images', 'blogs');
const dataDir = path.join(websiteDir, 'src', 'data');
const outputFile = path.join(dataDir, 'blogs.json');

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

const renderer = new marked.Renderer();
const defaultCodeRenderer = renderer.code.bind(renderer);

renderer.code = (token) => {
  const normalizedLang = token.lang?.match(/\S+/)?.[0]?.toLowerCase();

  if (normalizedLang === 'mermaid') {
    const diagramSource = token.text.replace(/\n$/, '');
    return `<pre class="mermaid">${escapeHtml(diagramSource)}</pre>\n`;
  }

  return defaultCodeRenderer(token);
};

marked.use({ renderer });

// Ensure output directories exist
fs.mkdirSync(imgDestDir, { recursive: true });
fs.mkdirSync(dataDir, { recursive: true });

// Copy blog images
if (fs.existsSync(imgSrcDir)) {
  const images = fs.readdirSync(imgSrcDir);
  for (const img of images) {
    fs.copyFileSync(path.join(imgSrcDir, img), path.join(imgDestDir, img));
  }
  console.log(`Copied ${images.length} images to public/images/blogs/`);
}

// Process markdown files
const mdFiles = fs.readdirSync(blogsDir).filter((f) => f.endsWith('.md'));
const blogs = [];

for (const file of mdFiles) {
  const raw = fs.readFileSync(path.join(blogsDir, file), 'utf-8');
  const { data: frontmatter, content } = matter(raw);

  // Convert markdown to HTML
  let html = marked.parse(content);

  // Fix image paths: ./img/ → /images/blogs/
  html = html.replace(/\.\/img\//g, '/images/blogs/');

  // Parse tags from comma-separated string to array
  const tags = frontmatter.tags
    ? String(frontmatter.tags)
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean)
    : [];

  // Generate plain-text excerpt (first 200 chars)
  const plainText = html
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  const excerpt = plainText.slice(0, 200);

  const blog = {
    slug: frontmatter.slug,
    title: frontmatter.title,
    datePublished: new Date(frontmatter.datePublished).toISOString(),
    tags,
    content: html,
    excerpt,
  };

  if (frontmatter.cover) {
    if (frontmatter.cover.startsWith('./img/')) {
      blog.cover = frontmatter.cover.replace('./img/', '/images/blogs/');
    } else {
      blog.cover = frontmatter.cover;
    }
  }

  blogs.push(blog);
}

// Sort newest first
blogs.sort((a, b) => new Date(b.datePublished).getTime() - new Date(a.datePublished).getTime());

fs.writeFileSync(outputFile, JSON.stringify(blogs, null, 2));
console.log(`Generated ${outputFile} with ${blogs.length} blog(s)`);
