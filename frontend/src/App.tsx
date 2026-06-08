import { FundingByRecipientDashboard } from './components/FundingByRecipientDashboard'
import './App.css'

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>Open Aid Transparency</h1>
        <p>Official development assistance by recipient country (World Bank ODA data)</p>
      </header>
      <main>
        <FundingByRecipientDashboard />
      </main>
    </div>
  )
}

export default App
