import { useEffect, useState } from 'react'
import { fetchFundingBySdg, type FundingBySdg } from '../api/reports'
import { FundingSdgChart } from './FundingSdgChart'

/**
 * "Funding by SDG category" analytic. Self-contained: fetches its own data and
 * re-fetches when refreshKey changes (after an admin import). Shows a helpful
 * prompt until some funding has been tagged with an SDG.
 */
export function FundingSdgSection({ refreshKey = 0 }: { refreshKey?: number }) {
  const [rows, setRows] = useState<FundingBySdg[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await fetchFundingBySdg()
        if (active) setRows(data)
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

  return (
    <>
      <h2>Funding by SDG category</h2>
      {loading && <p className="status">Loading…</p>}
      {error && <p className="status error">Could not load: {error}</p>}
      {!loading && !error && rows.length === 0 && (
        <p className="status">
          No categorised funding yet. Upload a funding CSV with an <code>sdgGoal</code> column
          (a goal number 1–17) to populate this chart.
        </p>
      )}
      {!loading && !error && rows.length > 0 && <FundingSdgChart rows={rows} />}
    </>
  )
}
