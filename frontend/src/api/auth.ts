// Authentication API client.

export interface AuthUser {
  username: string
  role: string
}

export interface LoginResult extends AuthUser {
  token: string
}

export async function login(username: string, password: string): Promise<LoginResult> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!response.ok) {
    throw new Error('Invalid username or password')
  }
  const data = await response.json()
  return { token: data.token, username: data.username, role: data.role }
}
