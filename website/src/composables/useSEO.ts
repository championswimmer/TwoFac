import { useHead } from '@unhead/vue'
import { toValue, type MaybeRefOrGetter } from 'vue'

const SITE_URL = 'https://twofac.app'
const DEFAULT_OG_IMAGE = `${SITE_URL}/twofac_logo_512.png`

type SEOValue<T> = MaybeRefOrGetter<T>

export function useSEO(options: {
  title: SEOValue<string>
  description: SEOValue<string>
  ogImage?: SEOValue<string | undefined>
  canonicalPath?: SEOValue<string | undefined>
  noindex?: SEOValue<boolean | undefined>
  ogType?: SEOValue<string | undefined>
  publishedTime?: SEOValue<string | undefined>
}) {
  useHead(() => {
    const title = toValue(options.title)
    const description = toValue(options.description)
    const ogImage = toValue(options.ogImage)
    const canonicalPath = toValue(options.canonicalPath)
    const noindex = toValue(options.noindex) ?? false
    const ogType = toValue(options.ogType) ?? 'website'
    const publishedTime = toValue(options.publishedTime)

    const resolvedImage = (ogImage ?? DEFAULT_OG_IMAGE).startsWith('http')
      ? (ogImage ?? DEFAULT_OG_IMAGE)
      : `${SITE_URL}${ogImage ?? DEFAULT_OG_IMAGE}`

    const canonicalUrl = canonicalPath ? `${SITE_URL}${canonicalPath}` : undefined
    const fullTitle = `${title} | TwoFac`

    return {
      title: fullTitle,
      meta: [
        { name: 'description', content: description },
        { name: 'robots', content: noindex ? 'noindex, nofollow' : 'index, follow' },
        { property: 'og:site_name', content: 'TwoFac' },
        { property: 'og:title', content: fullTitle },
        { property: 'og:description', content: description },
        { property: 'og:image', content: resolvedImage },
        { property: 'og:type', content: ogType },
        ...(canonicalUrl ? [{ property: 'og:url', content: canonicalUrl }] : []),
        ...(publishedTime ? [{ property: 'article:published_time', content: publishedTime }] : []),
        { name: 'twitter:card', content: 'summary_large_image' },
        { name: 'twitter:title', content: fullTitle },
        { name: 'twitter:description', content: description },
        { name: 'twitter:image', content: resolvedImage },
      ],
      link: canonicalUrl
        ? [{ rel: 'canonical', href: canonicalUrl }]
        : [],
    }
  })
}
