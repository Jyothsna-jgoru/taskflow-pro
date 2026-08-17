import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowRight, Plus, Users } from 'lucide-react'
import { Logo } from '../components/Logo'
import { useWorkspace } from '../context/WorkspaceContext'
import { useToast } from '../context/ToastContext'
import { api, errorMessage } from '../lib/api'
import type { Workspace } from '../types'
export function WorkspacePage() {
  const { workspaces, select } = useWorkspace()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const queryClient = useQueryClient()
  const { show } = useToast()
  const create = useMutation({
    mutationFn: async () => (await api.post<Workspace>('/workspaces', { name, description })).data,
    onSuccess: async (workspace) => {
      await queryClient.invalidateQueries({ queryKey: ['workspaces'] })
      show('Workspace created')
      select(workspace.id)
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  return (
    <div className="min-h-screen bg-[#f7f8fb] px-5 py-8">
      <div className="mx-auto max-w-5xl">
        <Logo />
        <div className="mt-16 grid gap-10 lg:grid-cols-[1fr_380px]">
          <section>
            <p className="text-xs font-bold uppercase tracking-[.18em] text-indigo-600">Your workspaces</p>
            <h1 className="mt-2 text-4xl font-extrabold tracking-tight">Where are you working today?</h1>
            <p className="mt-3 max-w-xl text-slate-500">
              Choose a team space or create a clean place for a new initiative.
            </p>
            <div className="mt-8 grid gap-3">
              {workspaces.map((w) => (
                <button
                  onClick={() => select(w.id)}
                  key={w.id}
                  className="panel group flex items-center gap-4 p-5 text-left transition hover:-translate-y-0.5 hover:border-indigo-200"
                >
                  <div className="grid size-12 place-items-center rounded-2xl bg-indigo-50 text-lg font-extrabold text-indigo-600">
                    {w.name[0]}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="font-extrabold text-slate-900">{w.name}</p>
                    <p className="mt-1 truncate text-sm text-slate-500">
                      {w.description || 'No description yet'}
                    </p>
                    <p className="mt-2 flex items-center gap-1 text-xs font-semibold text-slate-400">
                      <Users size={13} />
                      {w.memberCount} members · {w.currentUserRole}
                    </p>
                  </div>
                  <ArrowRight className="text-slate-300 transition group-hover:translate-x-1 group-hover:text-indigo-500" />
                </button>
              ))}
            </div>
          </section>
          <aside className="panel h-fit p-6">
            <div className="grid size-10 place-items-center rounded-xl bg-slate-900 text-white">
              <Plus size={20} />
            </div>
            <h2 className="mt-5 text-xl font-extrabold">Create a workspace</h2>
            <p className="mt-1 text-sm text-slate-500">You’ll become its admin automatically.</p>
            <div className="mt-5 space-y-4">
              <div>
                <label className="label">Workspace name</label>
                <input
                  className="input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Product studio"
                />
              </div>
              <div>
                <label className="label">Description</label>
                <textarea
                  className="input min-h-24 resize-none"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="What is this team working toward?"
                />
              </div>
              <button
                disabled={!name.trim() || create.isPending}
                onClick={() => create.mutate()}
                className="btn-primary w-full"
              >
                Create workspace
              </button>
            </div>
          </aside>
        </div>
      </div>
    </div>
  )
}
