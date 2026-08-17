import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Archive, ArrowUpRight, CalendarDays, FolderKanban, Plus, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { EmptyState, LoadingState, PageHeader } from '../components/States'
import { useWorkspace } from '../context/WorkspaceContext'
import { useToast } from '../context/ToastContext'
import { api, errorMessage } from '../lib/api'
import { shortDate, titleCase } from '../lib/format'
import type { Member, Project, ProjectStatus } from '../types'

export function ProjectsPage() {
  const { workspace } = useWorkspace()
  const { show } = useToast()
  const client = useQueryClient()
  const [form, setForm] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState<ProjectStatus>('PLANNING')
  const [ownerId, setOwnerId] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const projects = useQuery({
    queryKey: ['projects', workspace?.id],
    enabled: Boolean(workspace),
    queryFn: async () => (await api.get<Project[]>(`/workspaces/${workspace!.id}/projects`)).data,
  })
  const members = useQuery({
    queryKey: ['members', workspace?.id],
    enabled: Boolean(workspace),
    queryFn: async () => (await api.get<Member[]>(`/workspaces/${workspace!.id}/members`)).data,
  })
  const create = useMutation({
    mutationFn: async () =>
      (
        await api.post<Project>(`/workspaces/${workspace!.id}/projects`, {
          name,
          description,
          status,
          ownerId: ownerId || null,
          startDate: new Date().toISOString().slice(0, 10),
          targetDate: targetDate || null,
        })
      ).data,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['projects', workspace?.id] })
      show('Project created')
      setForm(false)
      setName('')
      setDescription('')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const archive = useMutation({
    mutationFn: async (id: string) => api.post(`/workspaces/${workspace!.id}/projects/${id}/archive`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['projects', workspace?.id] })
      show('Project archived')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  if (projects.isLoading) return <LoadingState label="Loading projects" />
  const active = projects.data?.filter((p) => p.status !== 'ARCHIVED') ?? []
  const canManage = workspace?.currentUserRole !== 'MEMBER'
  return (
    <>
      <PageHeader
        eyebrow="Portfolio"
        title="Projects"
        description="Coordinate outcomes, owners, and delivery timelines in one place."
        action={
          canManage ? (
            <button onClick={() => setForm(true)} className="btn-primary">
              <Plus size={17} />
              New project
            </button>
          ) : undefined
        }
      />
      {form && (
        <div className="panel mb-6 p-6">
          <div className="mb-5 flex items-start justify-between">
            <div>
              <h2 className="text-lg font-extrabold">Start a new project</h2>
              <p className="text-sm text-slate-500">
                Create the container your team will use to plan and deliver work.
              </p>
            </div>
            <button onClick={() => setForm(false)} className="text-slate-400">
              <X />
            </button>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            <div className="xl:col-span-2">
              <label className="label">Project name</label>
              <input
                className="input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Q4 customer experience"
              />
            </div>
            <div>
              <label className="label">Status</label>
              <select
                className="input"
                value={status}
                onChange={(e) => setStatus(e.target.value as ProjectStatus)}
              >
                {['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED'].map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Owner</label>
              <select className="input" value={ownerId} onChange={(e) => setOwnerId(e.target.value)}>
                <option value="">Unassigned</option>
                {members.data?.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.displayName}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Target date</label>
              <input
                className="input"
                type="date"
                value={targetDate}
                onChange={(e) => setTargetDate(e.target.value)}
              />
            </div>
            <div className="md:col-span-2 xl:col-span-4">
              <label className="label">Description</label>
              <input
                className="input"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Define the outcome and what success looks like"
              />
            </div>
            <div className="flex items-end">
              <button
                disabled={!name.trim() || create.isPending}
                onClick={() => create.mutate()}
                className="btn-primary w-full"
              >
                Create project
              </button>
            </div>
          </div>
        </div>
      )}
      {active.length ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {active.map((project) => (
            <article
              key={project.id}
              className="panel group p-5 transition hover:-translate-y-0.5 hover:border-indigo-200"
            >
              <div className="flex items-start justify-between">
                <div className="grid size-11 place-items-center rounded-2xl bg-indigo-50 text-indigo-600">
                  <FolderKanban size={21} />
                </div>
                <span
                  className={`badge ${project.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}
                >
                  {titleCase(project.status)}
                </span>
              </div>
              <Link
                to={`/projects/${project.id}`}
                className="mt-5 block text-lg font-extrabold text-slate-900 hover:text-indigo-600"
              >
                {project.name}
              </Link>
              <p className="mt-2 line-clamp-2 min-h-10 text-sm leading-relaxed text-slate-500">
                {project.description || 'No project description yet.'}
              </p>
              <div className="mt-5">
                <div className="flex justify-between text-xs">
                  <span className="font-semibold text-slate-400">Progress</span>
                  <strong className="text-slate-700">{project.progressPercentage}%</strong>
                </div>
                <div className="mt-2 h-2 rounded-full bg-slate-100">
                  <div
                    className="h-2 rounded-full bg-indigo-500"
                    style={{ width: `${project.progressPercentage}%` }}
                  />
                </div>
              </div>
              <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-4">
                <div className="flex items-center gap-4 text-xs text-slate-400">
                  <span className="flex items-center gap-1">
                    <CalendarDays size={14} />
                    {shortDate(project.targetDate)}
                  </span>
                  <span>{project.totalTasks} tasks</span>
                </div>
                <div className="flex items-center gap-2">
                  {canManage && (
                    <button
                      title="Archive project"
                      onClick={() => archive.mutate(project.id)}
                      className="text-slate-300 hover:text-rose-500"
                    >
                      <Archive size={16} />
                    </button>
                  )}
                  <Link to={`/projects/${project.id}`} className="text-indigo-600">
                    <ArrowUpRight size={17} />
                  </Link>
                </div>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState
          title="No active projects"
          description="Create your first project to turn a team goal into trackable work."
          action={
            canManage ? (
              <button onClick={() => setForm(true)} className="btn-primary">
                <Plus size={16} />
                Create project
              </button>
            ) : undefined
          }
        />
      )}
    </>
  )
}
