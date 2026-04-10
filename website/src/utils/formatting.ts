const blogDateFormatter = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  timeZone: 'UTC',
})

export function formatPublishedDate(dateString: string): string {
  return blogDateFormatter.format(new Date(dateString))
}
