import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { FundingByYear } from '../api/reports'

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

/** Area chart of total funding per year - the trend over time. */
export function FundingTrendChart({ rows }: { rows: FundingByYear[] }) {
  return (
    <div className="chart">
      <ResponsiveContainer width="100%" height={320}>
        <AreaChart data={rows} margin={{ top: 8, right: 16, bottom: 8, left: 8 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="year" />
          <YAxis tickFormatter={(v: number) => compactUsd.format(v)} width={70} />
          <Tooltip
            formatter={(value) => fullUsd.format(Number(value))}
            labelFormatter={(label) => `Year ${label}`}
          />
          <Area type="monotone" dataKey="totalAmount" name="Total funding" stroke="#2563eb" fill="#bfdbfe" />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
