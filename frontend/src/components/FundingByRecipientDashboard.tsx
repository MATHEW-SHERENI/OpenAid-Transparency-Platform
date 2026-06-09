import { lazy, Suspense, useEffect, useState } from 'react'
import { fetchFundingByRecipient, type FundingByRecipient } from '../api/reports'

// Lazy-loaded so the heavy charting (Recharts) and mapping (Leaflet) libraries
// land in their own JS chunks, fetched on demand instead of in the initial bundle.
const FundingChart = lazy(() => import('./FundingChart').then((m) => ({ default: m.FundingChart })))
const FundingMap = lazy(() => import('./FundingMap').then((m) => ({ default: m.FundingMap })))

const fullUsd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
})

const TOP_N = 10

export function FundingByRecipientDashboard({ refreshKey = 0 }: { refreshKey?: number }) {
  const [rows, setRows] = useState<FundingByRecipient[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Fetch on mount, and again whenever refreshKey changes (after an admin import).
  // The `active` guard discards a stale response if refreshKey changes mid-flight.
  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await fetchFundingByRecipient()
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

  if (loading) {
    return <p className="status">Loading funding data…</p>
  }
  if (error) {
    return <p className="status error">Could not load data: {error}</p>
  }
  if (rows.length === 0) {
    return <p className="status">No funding data yet. Run the World Bank ODA import first.</p>
  }

  // The API already sorts by amount descending; take the largest for the chart.
  const topRows = rows.slice(0, TOP_N)

  return (
    <>
      <h2>Funding by country</h2>
      <Suspense fallback={<p className="status">Loading map…</p>}>
        <FundingMap rows={rows} />
      </Suspense>

      <h2>Top {Math.min(TOP_N, rows.length)} recipients by total funding</h2>
      <Suspense fallback={<p className="status">Loading chart…</p>}>
        <FundingChart rows={topRows} />
      </Suspense>

      <h2>All recipients ({rows.length})</h2>
      <table className="report">
        <thead>
          <tr>
            <th>Country</th>
            <th>Currency</th>
            <th className="num">Total funding</th>
            <th className="num">Flows</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={`${r.recipientId}-${r.currency}`}>
              <td>{r.countryName}</td>
              <td>{r.currency}</td>
              <td className="num">{fullUsd.format(r.totalAmount)}</td>
              <td className="num">{r.flowCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}
