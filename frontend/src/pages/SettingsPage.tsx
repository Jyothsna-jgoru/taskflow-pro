import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Database, KeyRound, Save, Server, ShieldCheck } from 'lucide-react'
import { PageHeader } from '../components/States'
import { useWorkspace } from '../context/WorkspaceContext'
import { useToast } from '../context/ToastContext'
import { api, errorMessage } from '../lib/api'
import type { Workspace } from '../types'
export function SettingsPage() {
  const { workspace } = useWorkspace()
  const { show } = useToast()
  const client = useQueryClient()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  useEffect(() => {
    setName(workspace?.name ?? '')
    setDescription(workspace?.description ?? '')
  }, [workspace])
  const save = useMutation({
    mutationFn: async () =>
      (await api.put<Workspace>(`/workspaces/${workspace!.id}`, { name, description })).data,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['workspaces'] })
      show('Workspace settings saved')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const isAdmin = workspace?.currentUserRole === 'ADMIN'
  return (
    <>
      <PageHeader
        eyebrow="Workspace control"
        title="Settings"
        description="Review workspace identity, access model, and local infrastructure."
      />
      <div className="grid gap-5 xl:grid-cols-[1fr_380px]">
        <section className="panel p-6">
          <h2 className="text-lg font-extrabold">Workspace profile</h2>
          <p className="mt-1 text-sm text-slate-500">Shown to every member of this workspace.</p>
          <div className="mt-6 space-y-5">
            <div>
              <label className="label">Workspace name</label>
              <input
                className="input"
                disabled={!isAdmin}
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div>
              <label className="label">Description</label>
              <textarea
                className="input min-h-32 resize-none"
                disabled={!isAdmin}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div className="flex items-center justify-between border-t border-slate-100 pt-5">
              <p className="text-xs text-slate-400">Only workspace admins can change these details.</p>
              {isAdmin && (
                <button
                  disabled={!name.trim() || save.isPending}
                  onClick={() => save.mutate()}
                  className="btn-primary"
                >
                  <Save size={16} />
                  Save changes
                </button>
              )}
            </div>
          </div>
        </section>
        <aside className="space-y-5">
          <section className="panel p-6">
            <div className="flex items-center gap-3">
              <div className="grid size-10 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
                <ShieldCheck size={20} />
              </div>
              <div>
                <h2 className="font-extrabold">Security posture</h2>
                <p className="text-xs text-slate-400">Workspace-scoped RBAC</p>
              </div>
            </div>
            <div className="mt-5 space-y-3 text-sm">
              <div className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                <span className="flex items-center gap-2 text-slate-500">
                  <KeyRound size={15} />
                  Authentication
                </span>
                <strong className="text-slate-700">JWT / BCrypt</strong>
              </div>
              <div className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                <span className="flex items-center gap-2 text-slate-500">
                  <Server size={15} />
                  API role
                </span>
                <strong className="text-slate-700">{workspace?.currentUserRole}</strong>
              </div>
            </div>
          </section>
          <section className="panel p-6">
            <div className="flex items-center gap-3">
              <Database className="text-indigo-500" size={20} />
              <h2 className="font-extrabold">Local-first stack</h2>
            </div>
            <p className="mt-3 text-sm leading-relaxed text-slate-500">
              PostgreSQL stores durable work data. Redis accelerates dashboard and list reads. Both run
              locally in Docker without paid services.
            </p>
            <div className="mt-4 flex gap-2">
              <span className="badge bg-indigo-50 text-indigo-700">PostgreSQL</span>
              <span className="badge bg-rose-50 text-rose-700">Redis</span>
            </div>
          </section>
        </aside>
      </div>
    </>
  )
}
