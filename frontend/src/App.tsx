import { lazy, Suspense, useState } from 'react'
import { useAuth } from './auth/AuthContext'
import { LoginForm } from './components/LoginForm'
import { AdminPanel } from './components/AdminPanel'
import { FundingByRecipientDashboard } from './components/FundingByRecipientDashboard'
import './App.css'

const FundingSdgSection = lazy(() =>
  import('./components/FundingSdgSection').then((m) => ({ default: m.FundingSdgSection })),
)
const FundingTrendsSection = lazy(() =>
  import('./components/FundingTrendsSection').then((m) => ({ default: m.FundingTrendsSection })),
)

function App() {
  const { user, isAdmin, logout } = useAuth()
  // Bumping this re-fetches the dashboard (e.g. after an admin import).
  const [refreshKey, setRefreshKey] = useState(0)

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-row">
          <div>
            <h1>Open Aid Transparency</h1>
            <p>Official development assistance by recipient country (World Bank ODA data)</p>
          </div>
          <div className="auth-box">
            {user ? (
              <>
                <span className="who">
                  {user.username} <em>({user.role})</em>
                </span>
                <button onClick={logout}>Log out</button>
              </>
            ) : (
              <LoginForm />
            )}
          </div>
        </div>
      </header>

      <main>
        {isAdmin && <AdminPanel onDataChanged={() => setRefreshKey((k) => k + 1)} />}
        <FundingByRecipientDashboard refreshKey={refreshKey} />
        <Suspense fallback={<p className="status">Loading…</p>}>
          <FundingTrendsSection refreshKey={refreshKey} />
        </Suspense>
        <Suspense fallback={<p className="status">Loading…</p>}>
          <FundingSdgSection refreshKey={refreshKey} />
        </Suspense>
      </main>
    </div>
  )
}

export default App
