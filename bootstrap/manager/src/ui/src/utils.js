function formatTimestamp (inputTimestamp) {
  const dateObj = new Date(inputTimestamp)

  const timestamp = inputTimestamp !== null
    ? new Intl.DateTimeFormat(undefined, {
        dateStyle: 'short',
        timeStyle: 'short'
      }).format(dateObj).replace(',', '')
    : ''

  const timestampAgo = inputTimestamp !== null ? new Intl.RelativeTimeFormat().format(Math.floor((dateObj - new Date()) / 1000 / 60), 'minute') : 'Never'

  return {
    timestamp: timestamp,
    timestampAgo: timestampAgo
  }
}

export { formatTimestamp }
