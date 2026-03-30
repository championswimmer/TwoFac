import { useHead } from '@unhead/vue'

const SITE_URL = 'https://twofac.app'
const DEFAULT_OG_IMAGE = `${SITE_URL}/twofac_logo_512.png`

export function useSEO(options: {
  title: string
  description: string
  ogImage?: string
  canonicalPath?: string
}) {
  const { title, description, ogImage, canonicalPath } = options

  const resolvedImage = (ogImage ?? DEFAULT_OG_IMAGE).startsWith('http')
    ? (ogImage ?? DEFAULT_OG_IMAGE)
    : `${SITE_URL}${ogImage ?? DEFAULT_OG_IMAGE}`

  useHead({
    title: `${title} | TwoFac`,
    meta: [
      { name: 'description', content: description },
      { property: 'og:title', content: `${title} | TwoFac` },
      { property: 'og:description', content: description },
      { property: 'og:image', content: resolvedImage },
      { property: 'og:type', content: 'website' },
      { name: 'twitter:card', content: 'summary_large_image' },
    ],
    link: canonicalPath
      ? [{ rel: 'canonical', href: `${SITE_URL}${canonicalPath}` }]
      : [],
  })
}
