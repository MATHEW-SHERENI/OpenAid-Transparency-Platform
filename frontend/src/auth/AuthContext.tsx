import { createContext, useContext, useState, type ReactNode } from 'react'
import { login as apiLogin, type AuthUser } from '../api/auth'

interface AuthState {
  user: AuthUser | null
  token: string | null
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | undefined>(undefined)

// We persist the token in localStorage so a page refresh keeps the session.
// (Trade-off: localStorage is readable by any script, so it's vulnerable to XSS.
// For a stricter app you'd use an httpOnly cookie; fine for this learning project.)
const STORAGE_KEY = 'oat-auth'

interface Stored {
  user: AuthUser | null
  token: string | null
}

function load(): Stored {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Stored) : { user: null, token: null }
  } catch {
    return { user: null, token: null }
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<Stored>(load)

  const login = async (username: string, password: string) => {
    const result = await apiLogin(username, password)
    const next: Stored = { user: { username: result.username, role: result.role }, token: result.token }
    setState(next)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  }

  const logout = () => {
    setState({ user: null, token: null })
    localStorage.removeItem(STORAGE_KEY)
  }

  const value: AuthState = {
    user: state.user,
    token: state.token,
    isAdmin: state.user?.role === 'ADMIN',
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
