import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Clock3, Link2, Mail, Plus, ShieldCheck, Trash2, UserRound, X } from 'lucide-react'
import { EmptyState, LoadingState, PageHeader } from '../components/States'
import { useWorkspace } from '../context/WorkspaceContext'
import { useToast } from '../context/ToastContext'
import { api, errorMessage } from '../lib/api'
import { titleCase } from '../lib/format'
import type { InviteOrAddResponse, Member, Role, WorkspaceInvitation } from '../types'
export function TeamPage() {
  const { workspace } = useWorkspace()
  const { show } = useToast()
  const client = useQueryClient()
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Role>('MEMBER')
  const isAdmin = workspace?.currentUserRole === 'ADMIN'
  const members = useQuery({
    queryKey: ['members', workspace?.id],
    enabled: Boolean(workspace),
    queryFn: async () => (await api.get<Member[]>(`/workspaces/${workspace!.id}/members`)).data,
  })
  const invitations = useQuery({
    queryKey: ['workspace-invitations', workspace?.id],
    enabled: Boolean(workspace && isAdmin),
    queryFn: async () =>
      (await api.get<WorkspaceInvitation[]>(`/workspaces/${workspace!.id}/members/invitations`)).data,
  })
  const refresh = () => {
    client.invalidateQueries({ queryKey: ['members', workspace?.id] })
    client.invalidateQueries({ queryKey: ['workspaces'] })
    client.invalidateQueries({ queryKey: ['workspace-invitations', workspace?.id] })
  }
  const copyInvitationLink = async (token: string) => {
    const link = `${window.location.origin}/register?invite=${encodeURIComponent(token)}`
    try {
      await navigator.clipboard.writeText(link)
      show('Secure registration link copied to your clipboard')
    } catch {
      window.prompt('Copy this secure registration link:', link)
    }
  }
  const inviteOrAdd = useMutation({
    mutationFn: async () =>
      (
        await api.post<InviteOrAddResponse>(`/workspaces/${workspace!.id}/members/invite-or-add`, {
          email,
          role,
        })
      ).data,
    onSuccess: (result) => {
      refresh()
      setEmail('')
      if (result.action === 'ADDED') {
        show('Existing user added to the workspace')
      } else if (result.invitationToken) {
        void copyInvitationLink(result.invitationToken)
      }
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
  const cancelInvitation = useMutation({
    mutationFn: async (id: string) => api.delete(`/workspaces/${workspace!.id}/members/invitations/${id}`),
    onSuccess: () => {
      refresh()
      show('Pending invitation cancelled')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const regenerateInvitation = useMutation({
    mutationFn: async (id: string) =>
      (
        await api.post<InviteOrAddResponse>(
          `/workspaces/${workspace!.id}/members/invitations/${id}/regenerate`,
        )
      ).data,
    onSuccess: (result) => {
      refresh()
      if (result.invitationToken) void copyInvitationLink(result.invitationToken)
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  if (members.isLoading) return <LoadingState label="Loading team" />
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
              <h2 className="font-extrabold">Invite or add a teammate</h2>
              <p className="text-xs text-slate-400">
                Existing accounts join now. New email addresses receive a secure registration link.
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
              disabled={!email.trim() || inviteOrAdd.isPending}
              onClick={() => inviteOrAdd.mutate()}
              className="btn-primary"
            >
              Invite / add
            </button>
          </div>
          <p className="mt-3 text-xs text-slate-400">
            A secure registration link is copied after invitation creation. Share it through your normal team
            channel.
          </p>
        </section>
      )}
      {isAdmin && invitations.data?.length ? (
        <section className="panel mb-5 overflow-hidden">
          <div className="flex items-center justify-between gap-4 border-b border-slate-100 bg-amber-50/50 px-5 py-4">
            <div className="flex items-center gap-3">
              <div className="grid size-9 place-items-center rounded-xl bg-amber-100 text-amber-700">
                <Clock3 size={17} />
              </div>
              <div>
                <h2 className="text-sm font-extrabold">Pending invitations</h2>
                <p className="text-xs text-slate-500">
                  They activate when the recipient opens the secure link and creates an account.
                </p>
              </div>
            </div>
            <span className="badge bg-amber-100 text-amber-800">{invitations.data.length}</span>
          </div>
          <div className="divide-y divide-slate-100">
            {invitations.data.map((invitation) => (
              <div
                key={invitation.invitationId}
                className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="text-sm font-bold text-slate-800">{invitation.email}</p>
                  <p className="mt-0.5 text-xs text-slate-400">
                    {titleCase(invitation.role)} access · expires{' '}
                    {new Date(invitation.expiresAt).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button
                    aria-label={`Copy a new invitation link for ${invitation.email}`}
                    disabled={regenerateInvitation.isPending}
                    onClick={() => regenerateInvitation.mutate(invitation.invitationId)}
                    className="btn-secondary inline-flex items-center justify-center gap-2 py-2"
                  >
                    <Link2 size={15} /> Copy new link
                  </button>
                  <button
                    aria-label={`Cancel invitation for ${invitation.email}`}
                    disabled={cancelInvitation.isPending}
                    onClick={() => {
                      if (window.confirm(`Cancel the pending invitation for ${invitation.email}?`)) {
                        cancelInvitation.mutate(invitation.invitationId)
                      }
                    }}
                    className="btn-secondary inline-flex items-center justify-center gap-2 py-2 text-rose-600 hover:border-rose-200 hover:bg-rose-50"
                  >
                    <X size={15} /> Cancel
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      ) : null}
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
                      onClick={() => {
                        if (window.confirm(`Remove ${member.displayName} from this workspace?`)) {
                          remove.mutate(member.membershipId)
                        }
                      }}
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
        <EmptyState title="No members" description="Invite a teammate to begin collaborating." />
      )}
    </>
  )
}
