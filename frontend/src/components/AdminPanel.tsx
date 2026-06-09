import { useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import {
  importOda,
  importRecipients,
  importUnSdgGoals,
  uploadFundingCsv,
  type IngestionResult,
} from '../api/admin'

/**
 * Admin-only controls for importing data. Rendered only when the logged-in user
 * is an ADMIN (App decides), and the backend independently enforces the same.
 */
export function AdminPanel({ onDataChanged }: { onDataChanged: () => void }) {
  const { token } = useAuth()
  const [busy, setBusy] = useState<string | null>(null)
  const [result, setResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [file, setFile] = useState<File | null>(null)

  const run = async (label: string, action: () => Promise<IngestionResult>) => {
    setBusy(label)
    setError(null)
    setResult(null)
    try {
      const r = await action()
      setResult(`${label} — fetched ${r.fetched}, created ${r.created}, skipped ${r.skipped}`)
      onDataChanged() // refresh the dashboard so new data shows immediately
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed')
    } finally {
      setBusy(null)
    }
  }

  const uploadCsv = (e: FormEvent) => {
    e.preventDefault()
    if (file) run(`CSV "${file.name}"`, () => uploadFundingCsv(token!, file))
  }

  return (
    <section className="admin">
      <h2>Admin · data imports</h2>

      <div className="admin-actions">
        <button disabled={!!busy} onClick={() => run('World Bank recipients', () => importRecipients(token!))}>
          Import recipients
        </button>
        <button disabled={!!busy} onClick={() => run('World Bank ODA 2015–2022', () => importOda(token!, 2015, 2022))}>
          Import ODA funding
        </button>
        <button disabled={!!busy} onClick={() => run('UN SDG goals', () => importUnSdgGoals(token!))}>
          Refresh SDG goals (UN)
        </button>
      </div>

      <form className="upload" onSubmit={uploadCsv}>
        <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        <button type="submit" disabled={!!busy || !file}>
          Upload funding CSV
        </button>
      </form>
      <p className="hint">CSV columns: donor, recipientIso, year, amount, currency (optional: sdgGoal 1–17)</p>

      {busy && <p className="status">Running {busy}…</p>}
      {result && <p className="status ok">{result}</p>}
      {error && <p className="status error">{error}</p>}
    </section>
  )
}
