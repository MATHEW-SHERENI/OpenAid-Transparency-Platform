import { useEffect, useState } from 'react'
import {
  fetchFundingByDonor,
  fetchFundingByYear,
  type FundingByDonor,
  type FundingByYear,
} from '../api/reports'
import { FundingTrendChart } from './FundingTrendChart'
import { FundingByDonorChart } from './FundingByDonorChart'

/**
 * "Funding over time" + "Top donors" analytics. Self-contained: fetches both
 * reports in parallel and re-fetches when refreshKey changes (after an import).
 */
export function FundingTrendsSection({ refreshKey = 0 }: { refreshKey?: number }) {
  const [byYear, setByYear] = useState<FundingByYear[]>([])
  const [byDonor, setByDonor] = useState<FundingByDonor[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [years, donors] = await Promise.all([fetchFundingByYear(), fetchFundingByDonor()])
        if (active) {
          setByYear(years)
          setByDonor(donors)
        }
      } catch (e) {
        if (active) setError(e instanceof Error ? e.message : 'Unknown error')
      } finally {
        if (active) setLoading(false)
      }
    }
    void load()
    return () => {
      active = false
    }
  }, [refreshKey])

  if (loading) {
    return <p className="status">Loading…</p>
  }
  if (error) {
    return <p className="status error">Could not load: {error}</p>
  }
  // No funding yet - the recipient dashboard already prompts the admin to import.
  if (byYear.length === 0) {
    return null
  }

  return (
    <>
      <h2>Funding over time</h2>
      <FundingTrendChart rows={byYear} />

      <h2>Top donors</h2>
      <FundingByDonorChart rows={byDonor} />
    </>
  )
}
