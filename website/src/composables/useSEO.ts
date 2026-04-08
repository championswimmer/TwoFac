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

  const canonicalUrl = canonicalPath ? `${SITE_URL}${canonicalPath}` : undefined

  useHead({
    title: `${title} | TwoFac`,
    meta: [
      { name: 'description', content: description },
      { property: 'og:title', content: `${title} | TwoFac` },
      { property: 'og:description', content: description },
      { property: 'og:image', content: resolvedImage },
      { property: 'og:type', content: 'website' },
      ...(canonicalUrl ? [{ property: 'og:url', content: canonicalUrl }] : []),
      { name: 'twitter:card', content: 'summary_large_image' },
      { name: 'twitter:title', content: `${title} | TwoFac` },
      { name: 'twitter:description', content: description },
      { name: 'twitter:image', content: resolvedImage },
    ],
    link: canonicalUrl
      ? [{ rel: 'canonical', href: canonicalUrl }]
      : [],
  })
}
