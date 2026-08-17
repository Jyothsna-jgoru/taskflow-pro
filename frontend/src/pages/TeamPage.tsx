import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Mail, Plus, ShieldCheck, Trash2, UserRound } from 'lucide-react'
import { EmptyState, LoadingState, PageHeader } from '../components/States'
import { useWorkspace } from '../context/WorkspaceContext'
import { useToast } from '../context/ToastContext'
import { api, errorMessage } from '../lib/api'
import { titleCase } from '../lib/format'
import type { Member, Role } from '../types'
export function TeamPage() {
  const { workspace } = useWorkspace()
  const { show } = useToast()
  const client = useQueryClient()
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Role>('MEMBER')
  const members = useQuery({
    queryKey: ['members', workspace?.id],
    enabled: Boolean(workspace),
    queryFn: async () => (await api.get<Member[]>(`/workspaces/${workspace!.id}/members`)).data,
  })
  const refresh = () => {
    client.invalidateQueries({ queryKey: ['members', workspace?.id] })
    client.invalidateQueries({ queryKey: ['workspaces'] })
  }
  const add = useMutation({
    mutationFn: async () => api.post(`/workspaces/${workspace!.id}/members`, { email, role }),
    onSuccess: () => {
      refresh()
      setEmail('')
      show('Member added')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const update = useMutation({
    mutationFn: async ({ id, role }: { id: string; role: Role }) =>
      api.patch(`/workspaces/${workspace!.id}/members/${id}`, { role }),
    onSuccess: () => {
      refresh()
      show('Role updated')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/workspaces/${workspace!.id}/members/${id}`),
    onSuccess: () => {
      refresh()
      show('Member removed')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  if (members.isLoading) return <LoadingState label="Loading team" />
  const isAdmin = workspace?.currentUserRole === 'ADMIN'
  return (
    <>
      <PageHeader
        eyebrow="People & access"
        title="Team members"
        description="Manage who belongs to this workspace and what they are allowed to change."
      />
      {isAdmin && (
        <section className="panel mb-5 p-5">
          <div className="flex items-center gap-3">
            <div className="grid size-10 place-items-center rounded-xl bg-indigo-50 text-indigo-600">
              <Plus size={19} />
            </div>
            <div>
              <h2 className="font-extrabold">Add a registered user</h2>
              <p className="text-xs text-slate-400">
                For this local build, the user must create an account before being added.
              </p>
            </div>
          </div>
          <div className="mt-5 flex flex-col gap-3 sm:flex-row">
            <input
              className="input flex-1"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="teammate@example.com"
            />
            <select className="input sm:w-44" value={role} onChange={(e) => setRole(e.target.value as Role)}>
              {['MEMBER', 'MANAGER', 'ADMIN'].map((r) => (
                <option key={r}>{r}</option>
              ))}
            </select>
            <button
              disabled={!email.trim() || add.isPending}
              onClick={() => add.mutate()}
              className="btn-primary"
            >
              Add member
            </button>
          </div>
        </section>
      )}
      {members.data?.length ? (
        <section className="panel overflow-hidden">
          <div className="hidden grid-cols-[1fr_1fr_180px_80px] gap-4 border-b border-slate-100 bg-slate-50/70 px-6 py-3 text-[10px] font-bold uppercase tracking-[.14em] text-slate-400 md:grid">
            <span>Member</span>
            <span>Contact</span>
            <span>Workspace role</span>
            <span>Action</span>
          </div>
          <div className="divide-y divide-slate-100">
            {members.data.map((member) => (
              <div
                key={member.membershipId}
                className="grid gap-4 px-5 py-4 md:grid-cols-[1fr_1fr_180px_80px] md:items-center md:px-6"
              >
                <div className="flex items-center gap-3">
                  <div className="grid size-10 place-items-center rounded-full bg-slate-100 text-xs font-extrabold text-slate-700">
                    <UserRound size={17} />
                  </div>
                  <div>
                    <p className="text-sm font-extrabold text-slate-800">{member.displayName}</p>
                    <p className="text-[11px] text-slate-400">
                      Joined {new Date(member.joinedAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 text-sm text-slate-500">
                  <Mail size={15} />
                  {member.email}
                </div>
                <div>
                  {isAdmin ? (
                    <select
                      aria-label={`Role for ${member.displayName}`}
                      className="input py-2"
                      value={member.role}
                      onChange={(e) =>
                        update.mutate({ id: member.membershipId, role: e.target.value as Role })
                      }
                    >
                      {['MEMBER', 'MANAGER', 'ADMIN'].map((r) => (
                        <option key={r}>{r}</option>
                      ))}
                    </select>
                  ) : (
                    <span
                      className={`badge ${member.role === 'ADMIN' ? 'bg-indigo-50 text-indigo-700' : 'bg-slate-100 text-slate-600'}`}
                    >
                      <ShieldCheck size={12} className="mr-1" />
                      {titleCase(member.role)}
                    </span>
                  )}
                </div>
                <div>
                  {isAdmin && (
                    <button
                      aria-label={`Remove ${member.displayName}`}
                      onClick={() => remove.mutate(member.membershipId)}
                      className="rounded-lg p-2 text-slate-300 hover:bg-rose-50 hover:text-rose-600"
                    >
                      <Trash2 size={17} />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      ) : (
        <EmptyState title="No members" description="Add a registered user to begin collaborating." />
      )}
    </>
  )
}
