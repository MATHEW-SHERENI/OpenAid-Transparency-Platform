import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { FundingByRecipient } from '../api/reports'

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

/**
 * The Recharts bar chart, in its own module so it can be lazy-loaded - this keeps
 * the (heavy) charting library out of the initial JS bundle.
 */
export function FundingChart({ rows }: { rows: FundingByRecipient[] }) {
  return (
    <div className="chart">
      <ResponsiveContainer width="100%" height={380}>
        <BarChart data={rows} margin={{ top: 8, right: 16, bottom: 64, left: 8 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="countryName" angle={-40} textAnchor="end" interval={0} height={70} />
          <YAxis tickFormatter={(v: number) => compactUsd.format(v)} width={70} />
          <Tooltip formatter={(value) => fullUsd.format(Number(value))} />
          <Bar dataKey="totalAmount" name="Total funding" fill="#2563eb" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
