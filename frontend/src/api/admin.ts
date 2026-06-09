// Admin-only API calls. Each attaches the JWT as a Bearer token; the backend
// rejects non-ADMIN callers with 403.

export interface IngestionResult {
  fetched: number
  created: number
  skipped: number
}

async function authedPost(
  path: string,
  token: string,
  body?: BodyInit,
): Promise<IngestionResult> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    // NOTE: for the FormData upload we deliberately do NOT set Content-Type -
    // the browser sets it (with the multipart boundary) automatically.
    body,
  })
  if (!response.ok) {
    throw new Error(`Request failed (HTTP ${response.status})`)
  }
  return response.json()
}

export function importRecipients(token: string): Promise<IngestionResult> {
  return authedPost('/api/ingestion/recipients/world-bank', token)
}

export function importOda(token: string, fromYear: number, toYear: number): Promise<IngestionResult> {
  return authedPost(`/api/ingestion/funding/world-bank-oda?fromYear=${fromYear}&toYear=${toYear}`, token)
}

export function importUnSdgGoals(token: string): Promise<IngestionResult> {
  return authedPost('/api/ingestion/sdg-goals/un', token)
}

export function uploadFundingCsv(token: string, file: File): Promise<IngestionResult> {
  const form = new FormData()
  form.append('file', file)
  return authedPost('/api/ingestion/funding/csv', token, form)
}
