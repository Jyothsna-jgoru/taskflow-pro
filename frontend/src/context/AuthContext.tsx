import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../lib/api'
import type { AuthResponse, User } from '../types'

interface AuthValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (displayName: string, email: string, password: string, invitationToken?: string) => Promise<void>
  logout: () => void
}
const AuthContext = createContext<AuthValue | null>(null)
const storedUser = () => {
  try {
    return JSON.parse(localStorage.getItem('taskflow_user') || 'null') as User | null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(storedUser)
  const [loading, setLoading] = useState(Boolean(localStorage.getItem('taskflow_token')))
  useEffect(() => {
    const token = localStorage.getItem('taskflow_token')
    if (!token) {
      setLoading(false)
      return
    }
    api
      .get<User>('/auth/me')
      .then(({ data }) => {
        setUser(data)
        localStorage.setItem('taskflow_user', JSON.stringify(data))
      })
      .catch(() => {
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [])
  useEffect(() => {
    const unauthorized = () => {
      setUser(null)
      setLoading(false)
    }
    window.addEventListener('taskflow:unauthorized', unauthorized)
    return () => window.removeEventListener('taskflow:unauthorized', unauthorized)
  }, [])
  const store = (data: AuthResponse) => {
    localStorage.setItem('taskflow_token', data.accessToken)
    localStorage.setItem('taskflow_user', JSON.stringify(data.user))
    setUser(data.user)
  }
  const value = useMemo<AuthValue>(
    () => ({
      user,
      loading,
      login: async (email, password) =>
        store((await api.post<AuthResponse>('/auth/login', { email, password })).data),
      register: async (displayName, email, password, invitationToken) =>
        store(
          (
            await api.post<AuthResponse>('/auth/register', {
              displayName,
              email,
              password,
              invitationToken,
            })
          ).data,
        ),
      logout: () => {
        api.post('/auth/logout').catch(() => undefined)
        localStorage.removeItem('taskflow_token')
        localStorage.removeItem('taskflow_user')
        localStorage.removeItem('taskflow_workspace')
        setUser(null)
      },
    }),
    [user, loading],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export const useAuth = () => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth requires AuthProvider')
  return value
}
