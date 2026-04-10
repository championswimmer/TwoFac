<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import { useSEO } from '../composables/useSEO'
import blogs from '../data/blogs.json'
import { formatPublishedDate } from '../utils/formatting'

const route = useRoute()
const post = computed(() => blogs.find((b) => b.slug === route.params.slug))
const blogContent = ref<HTMLElement | null>(null)

useSEO({
  title: computed(() => post.value?.title ?? 'Post Not Found'),
  description: computed(() => post.value?.excerpt ?? 'The requested blog post could not be found.'),
  canonicalPath: computed(() => (post.value ? `/blog/${post.value.slug}` : undefined)),
  noindex: computed(() => !post.value),
  ogType: computed(() => (post.value ? 'article' : 'website')),
  publishedTime: computed(() => post.value?.datePublished),
})

function getMermaidTheme(): 'default' | 'dark' {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'default'
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'default'
}

async function renderMermaidDiagrams(): Promise<void> {
  if (import.meta.env.SSR || !blogContent.value) {
    return
  }

  const mermaidBlocks = Array.from(blogContent.value.querySelectorAll<HTMLElement>('pre.mermaid'))

  if (mermaidBlocks.length === 0) {
    return
  }

  const mermaid = (await import('mermaid')).default

  mermaid.initialize({
    startOnLoad: false,
    theme: getMermaidTheme(),
    htmlLabels: true,
    securityLevel: 'loose',
  })

  await mermaid.run({ nodes: mermaidBlocks })
}

function queueMermaidRender(): void {
  if (import.meta.env.SSR) {
    return
  }

  void renderMermaidDiagrams().catch((error: unknown) => {
    console.error('Failed to render Mermaid diagrams for blog post content.', error)
  })
}

onMounted(() => {
  queueMermaidRender()
})

watch(
  () => post.value?.content,
  () => {
    queueMermaidRender()
  },
  { flush: 'post' },
)
</script>

<template>
  <MainLayout>
    <!-- Post found -->
    <article v-if="post" class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <RouterLink
        to="/blog"
        class="inline-flex items-center text-sm font-medium text-primary-600 dark:text-primary-400 hover:underline mb-8"
      >
        ← Back to Blog
      </RouterLink>

      <!-- Header -->
      <header class="mb-8">
        <time
          :datetime="post.datePublished"
          class="text-sm text-secondary-500 dark:text-secondary-400"
        >
          {{ formatPublishedDate(post.datePublished) }}
        </time>

        <h1 class="mt-2 text-3xl sm:text-4xl font-bold text-secondary-900 dark:text-white leading-tight">
          {{ post.title }}
        </h1>

        <div class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="tag in post.tags"
            :key="tag"
            class="inline-block px-2.5 py-0.5 text-xs font-medium rounded-full bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300"
          >
            {{ tag }}
          </span>
        </div>
      </header>

      <!-- Content -->
      <div ref="blogContent" class="blog-content" v-html="post.content" />
    </article>

    <!-- Post not found -->
    <div v-else class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-24 text-center">
      <h1 class="text-4xl font-bold text-secondary-900 dark:text-white mb-4">404</h1>
      <p class="text-lg text-secondary-600 dark:text-secondary-400 mb-8">
        The blog post you're looking for could not be found.
      </p>
      <RouterLink
        to="/blog"
        class="inline-flex items-center px-6 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors"
      >
        ← Back to Blog
      </RouterLink>
    </div>
  </MainLayout>
</template>

<style scoped>
/* Typography styles for blog HTML content */
.blog-content :deep(h1) {
  font-size: 2rem;
  font-weight: 700;
  margin-top: 2.5rem;
  margin-bottom: 1rem;
  line-height: 1.2;
  color: var(--color-secondary-900);
}
.blog-content :deep(h2) {
  font-size: 1.5rem;
  font-weight: 600;
  margin-top: 2rem;
  margin-bottom: 0.75rem;
  line-height: 1.3;
  color: var(--color-secondary-900);
}
.blog-content :deep(h3) {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 1.75rem;
  margin-bottom: 0.5rem;
  line-height: 1.4;
  color: var(--color-secondary-900);
}
.blog-content :deep(p) {
  margin-bottom: 1.25rem;
  line-height: 1.75;
}
.blog-content :deep(a) {
  color: var(--color-primary-600);
  text-decoration: underline;
  text-underline-offset: 2px;
}
.blog-content :deep(a:hover) {
  color: var(--color-primary-700);
}
.blog-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 0.75rem;
  margin: 1.5rem 0;
}
.blog-content :deep(code) {
  font-family: var(--font-mono);
  font-size: 0.875em;
  padding: 0.15em 0.4em;
  border-radius: 0.375rem;
  background-color: var(--color-secondary-100);
  color: var(--color-secondary-800);
}
.blog-content :deep(pre) {
  font-family: var(--font-mono);
  font-size: 0.875rem;
  padding: 1rem 1.25rem;
  border-radius: 0.75rem;
  background-color: var(--color-secondary-100);
  color: var(--color-secondary-800);
  overflow-x: auto;
  margin: 1.5rem 0;
}
.blog-content :deep(pre.mermaid) {
  padding: 0.75rem;
  background-color: transparent;
  color: inherit;
  border: 1px solid var(--color-secondary-200);
}
.blog-content :deep(pre.mermaid svg) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
}
.blog-content :deep(pre code) {
  padding: 0;
  background: none;
}
.blog-content :deep(ul),
.blog-content :deep(ol) {
  margin-bottom: 1.25rem;
  padding-left: 1.5rem;
}
.blog-content :deep(ul) {
  list-style-type: disc;
}
.blog-content :deep(ol) {
  list-style-type: decimal;
}
.blog-content :deep(li) {
  margin-bottom: 0.5rem;
  line-height: 1.75;
}
.blog-content :deep(blockquote) {
  border-left: 4px solid var(--color-primary-400);
  padding-left: 1rem;
  margin: 1.5rem 0;
  color: var(--color-secondary-600);
  font-style: italic;
}
.blog-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--color-secondary-200);
  margin: 2rem 0;
}
.blog-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
}
.blog-content :deep(th),
.blog-content :deep(td) {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-secondary-200);
  text-align: left;
}
.blog-content :deep(th) {
  background-color: var(--color-secondary-50);
  font-weight: 600;
}

/* Dark mode overrides */
@media (prefers-color-scheme: dark) {
  .blog-content :deep(h1),
  .blog-content :deep(h2),
  .blog-content :deep(h3) {
    color: white;
  }
  .blog-content :deep(a) {
    color: var(--color-primary-400);
  }
  .blog-content :deep(a:hover) {
    color: var(--color-primary-300);
  }
  .blog-content :deep(code) {
    background-color: var(--color-secondary-800);
    color: var(--color-secondary-200);
  }
  .blog-content :deep(pre) {
    background-color: var(--color-secondary-800);
    color: var(--color-secondary-200);
  }
  .blog-content :deep(pre.mermaid) {
    background-color: transparent;
    border-color: var(--color-secondary-700);
  }
  .blog-content :deep(blockquote) {
    color: var(--color-secondary-400);
  }
  .blog-content :deep(hr) {
    border-color: var(--color-secondary-700);
  }
  .blog-content :deep(th),
  .blog-content :deep(td) {
    border-color: var(--color-secondary-700);
  }
  .blog-content :deep(th) {
    background-color: var(--color-secondary-800);
  }
}
</style>
