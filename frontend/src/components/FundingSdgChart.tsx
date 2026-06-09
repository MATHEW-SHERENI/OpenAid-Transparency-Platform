import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import type { FundingBySdg } from '../api/reports'

const fullUsd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
})

// A distinct colour per category slice.
const COLORS = [
  '#e5243b', '#dda63a', '#4c9f38', '#c5192d', '#ff3a21', '#26bde2',
  '#fcc30b', '#a21942', '#fd6925', '#dd1367', '#fd9d24', '#bf8b2e',
  '#3f7e44', '#0a97d9', '#56c02b', '#00689d', '#19486a',
]

/**
 * Donut chart of funding split by SDG category - one slice per goal. Its own
 * module so it (and Recharts) can be lazy-loaded.
 */
export function FundingSdgChart({ rows }: { rows: FundingBySdg[] }) {
  const data = rows.map((r) => ({ ...r, label: `SDG ${r.goalNumber}: ${r.goalTitle}` }))

  return (
    <div className="chart">
      <ResponsiveContainer width="100%" height={360}>
        <PieChart>
          <Pie
            data={data}
            dataKey="totalAmount"
            nameKey="label"
            innerRadius={70}
            outerRadius={130}
            paddingAngle={2}
          >
            {data.map((r) => (
              <Cell key={r.goalNumber} fill={COLORS[(r.goalNumber - 1) % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip formatter={(value) => fullUsd.format(Number(value))} />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
