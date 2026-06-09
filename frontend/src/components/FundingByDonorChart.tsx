import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { FundingByDonor } from '../api/reports'

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

/** Horizontal bar chart of the top donors by total funding. */
export function FundingByDonorChart({ rows }: { rows: FundingByDonor[] }) {
  const data = rows.slice(0, TOP_N)

  return (
    <div className="chart">
      <ResponsiveContainer width="100%" height={Math.max(240, data.length * 40)}>
        <BarChart data={data} layout="vertical" margin={{ top: 8, right: 24, bottom: 8, left: 8 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis type="number" tickFormatter={(v: number) => compactUsd.format(v)} />
          <YAxis type="category" dataKey="donorName" width={190} tick={{ fontSize: 12 }} />
          <Tooltip formatter={(value) => fullUsd.format(Number(value))} />
          <Bar dataKey="totalAmount" name="Total funding" fill="#7c3aed" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
