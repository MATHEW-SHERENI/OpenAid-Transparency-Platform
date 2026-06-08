import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { fetchFundingByRecipient, type FundingByRecipient } from '../api/reports'

// Compact money formatting for axis/labels, e.g. 18996950927 -> "$19B".
const compactUsd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  notation: 'compact',
  maximumFractionDigits: 1,
})
const fullUsd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
})

const TOP_N = 10

export function FundingByRecipientDashboard() {
  const [rows, setRows] = useState<FundingByRecipient[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Fetch once when the component mounts.
  useEffect(() => {
    fetchFundingByRecipient()
      .then(setRows)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Unknown error'))
      .finally(() => setLoading(false))
  }, [])

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
      <h2>Top {Math.min(TOP_N, rows.length)} recipients by total funding</h2>
      <div className="chart">
        <ResponsiveContainer width="100%" height={380}>
          <BarChart data={topRows} margin={{ top: 8, right: 16, bottom: 64, left: 8 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="countryName" angle={-40} textAnchor="end" interval={0} height={70} />
            <YAxis tickFormatter={(v: number) => compactUsd.format(v)} width={70} />
            <Tooltip formatter={(value) => fullUsd.format(Number(value))} />
            <Bar dataKey="totalAmount" name="Total funding" fill="#2563eb" />
          </BarChart>
        </ResponsiveContainer>
      </div>

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
