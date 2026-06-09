// Typed client for the funding report endpoints.
//
// The interface mirrors the backend's FundingByRecipient record, so TypeScript
// catches us if we read a field the API doesn't return. Requests use a relative
// /api path, which Vite proxies to the Spring backend in dev (see vite.config.ts).

export interface FundingByRecipient {
  recipientId: number
  isoCode: string | null
  countryName: string
  currency: string
  totalAmount: number
  flowCount: number
}

export async function fetchFundingByRecipient(): Promise<FundingByRecipient[]> {
  const response = await fetch('/api/funding-flows/reports/by-recipient')
  if (!response.ok) {
    throw new Error(`Failed to load funding report (HTTP ${response.status})`)
  }
  return response.json()
}

export interface FundingBySdg {
  goalNumber: number
  goalTitle: string
  totalAmount: number
  flowCount: number
}

export async function fetchFundingBySdg(): Promise<FundingBySdg[]> {
  const response = await fetch('/api/funding-flows/reports/by-sdg')
  if (!response.ok) {
    throw new Error(`Failed to load SDG report (HTTP ${response.status})`)
  }
  return response.json()
}
