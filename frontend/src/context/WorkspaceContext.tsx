import { createContext, useContext, useEffect, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { Workspace } from '../types'

interface WorkspaceValue {
  workspaces: Workspace[]
  workspace: Workspace | null
  loading: boolean
  select: (id: string) => void
}
const WorkspaceContext = createContext<WorkspaceValue | null>(null)
export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const query = useQuery({
    queryKey: ['workspaces'],
    queryFn: async () => (await api.get<Workspace[]>('/workspaces')).data,
  })
  const selected = localStorage.getItem('taskflow_workspace')
  const workspace = query.data?.find((w) => w.id === selected) ?? null
  useEffect(() => {
    if (query.data?.length && !workspace) {
      localStorage.setItem('taskflow_workspace', query.data[0].id)
    }
  }, [query.data, workspace])
  return (
    <WorkspaceContext.Provider
      value={{
        workspaces: query.data ?? [],
        workspace: workspace ?? query.data?.[0] ?? null,
        loading: query.isLoading,
        select: (id) => {
          localStorage.setItem('taskflow_workspace', id)
          window.dispatchEvent(new Event('taskflow:workspace'))
          window.location.assign('/')
        },
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  )
}
export const useWorkspace = () => {
  const value = useContext(WorkspaceContext)
  if (!value) throw new Error('useWorkspace requires WorkspaceProvider')
  return value
}
