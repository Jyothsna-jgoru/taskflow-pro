import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarDays, CheckCircle2, CircleDot, Pencil, UserRound } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { EmptyState, LoadingState, PageHeader } from '../components/States'
import { useWorkspace } from '../context/WorkspaceContext'
import { useToast } from '../context/ToastContext'
import { api, errorMessage } from '../lib/api'
import { shortDate, statusTone, titleCase } from '../lib/format'
import type { Member, Project, ProjectStatus, TaskPage } from '../types'
export function ProjectDetailPage() {
  const { projectId } = useParams()
  const { workspace } = useWorkspace()
  const { show } = useToast()
  const client = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState<ProjectStatus>('PLANNING')
  const [ownerId, setOwnerId] = useState('')
  const [startDate, setStartDate] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const project = useQuery({
    queryKey: ['project', workspace?.id, projectId],
    enabled: Boolean(workspace && projectId),
    queryFn: async () => (await api.get<Project>(`/workspaces/${workspace!.id}/projects/${projectId}`)).data,
  })
  const tasks = useQuery({
    queryKey: ['tasks', workspace?.id, 'project', projectId],
    enabled: Boolean(workspace && projectId),
    queryFn: async () =>
      (await api.get<TaskPage>(`/workspaces/${workspace!.id}/tasks`, { params: { projectId, size: 100 } }))
        .data,
  })
  const members = useQuery({
    queryKey: ['members', workspace?.id],
    enabled: Boolean(workspace),
    queryFn: async () => (await api.get<Member[]>(`/workspaces/${workspace!.id}/members`)).data,
  })
  useEffect(() => {
    if (!project.data) return
    setName(project.data.name)
    setDescription(project.data.description ?? '')
    setStatus(project.data.status)
    setOwnerId(project.data.owner?.id ?? '')
    setStartDate(project.data.startDate ?? '')
    setTargetDate(project.data.targetDate ?? '')
  }, [project.data])
  const update = useMutation({
    mutationFn: async () =>
      (
        await api.put<Project>(`/workspaces/${workspace!.id}/projects/${projectId}`, {
          name,
          description,
          status,
          ownerId: ownerId || null,
          startDate: startDate || null,
          targetDate: targetDate || null,
        })
      ).data,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['project', workspace?.id, projectId] })
      client.invalidateQueries({ queryKey: ['projects', workspace?.id] })
      client.invalidateQueries({ queryKey: ['dashboard', workspace?.id] })
      setEditing(false)
      show('Project updated')
    },
    onError: (error) => show(errorMessage(error), 'error'),
  })
  if (project.isLoading) return <LoadingState label="Loading project" />
  if (!project.data)
    return (
      <EmptyState
        title="Project not found"
        description="It may have been archived or you may no longer have access."
      />
    )
  const p = project.data
  const canManage = workspace?.currentUserRole !== 'MEMBER'
  return (
    <>
      <Link
        to="/projects"
        className="mb-5 inline-flex items-center gap-2 text-sm font-bold text-slate-500 hover:text-indigo-600"
      >
        <ArrowLeft size={16} />
        All projects
      </Link>
      <PageHeader
        eyebrow={titleCase(p.status)}
        title={p.name}
        description={p.description || 'No project description.'}
        action={
          <div className="flex gap-2">
            {canManage && (
              <button onClick={() => setEditing((value) => !value)} className="btn-secondary">
                <Pencil size={16} />
                Edit project
              </button>
            )}
            <Link to={`/board?projectId=${p.id}`} className="btn-primary">
              Open on board
            </Link>
          </div>
        }
      />
      {editing && (
        <section className="panel mb-5 grid gap-4 p-6 md:grid-cols-2 xl:grid-cols-3">
          <div>
            <label className="label">Project name</label>
            <input className="input" value={name} onChange={(event) => setName(event.target.value)} />
          </div>
          <div>
            <label className="label">Status</label>
            <select
              className="input"
              value={status}
              onChange={(event) => setStatus(event.target.value as ProjectStatus)}
            >
              {['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED'].map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label">Owner</label>
            <select className="input" value={ownerId} onChange={(event) => setOwnerId(event.target.value)}>
              <option value="">Unassigned</option>
              {members.data?.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.displayName}
                </option>
              ))}
            </select>
          </div>
          <div className="md:col-span-2 xl:col-span-3">
            <label className="label">Description</label>
            <textarea
              className="input min-h-24"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </div>
          <div>
            <label className="label">Start date</label>
            <input
              type="date"
              className="input"
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
            />
          </div>
          <div>
            <label className="label">Target date</label>
            <input
              type="date"
              className="input"
              value={targetDate}
              onChange={(event) => setTargetDate(event.target.value)}
            />
          </div>
          <div className="flex items-end justify-end gap-2">
            <button className="btn-secondary" onClick={() => setEditing(false)}>
              Cancel
            </button>
            <button
              className="btn-primary"
              disabled={!name.trim() || update.isPending}
              onClick={() => update.mutate()}
            >
              Save project
            </button>
          </div>
        </section>
      )}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="panel p-5">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-400">Progress</p>
          <p className="mt-2 text-3xl font-extrabold">{p.progressPercentage}%</p>
          <div className="mt-3 h-2 rounded-full bg-slate-100">
            <div className="h-2 rounded-full bg-emerald-500" style={{ width: `${p.progressPercentage}%` }} />
          </div>
        </div>
        <div className="panel p-5">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-400">Owner</p>
          <p className="mt-3 flex items-center gap-2 text-sm font-bold">
            <UserRound size={17} className="text-indigo-500" />
            {p.owner?.displayName || 'Unassigned'}
          </p>
        </div>
        <div className="panel p-5">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-400">Target date</p>
          <p className="mt-3 flex items-center gap-2 text-sm font-bold">
            <CalendarDays size={17} className="text-amber-500" />
            {shortDate(p.targetDate)}
          </p>
        </div>
        <div className="panel p-5">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-400">Delivery</p>
          <p className="mt-3 flex items-center gap-2 text-sm font-bold">
            <CheckCircle2 size={17} className="text-emerald-500" />
            {p.completedTasks} of {p.totalTasks} complete
          </p>
        </div>
      </section>
      <section className="panel mt-5 p-6">
        <div className="mb-5 flex items-center justify-between">
          <div>
            <h2 className="font-extrabold">Project tasks</h2>
            <p className="text-xs text-slate-400">The latest work in this project</p>
          </div>
          <CircleDot className="text-indigo-500" size={19} />
        </div>
        {tasks.data?.content.length ? (
          <div className="divide-y divide-slate-100">
            {tasks.data.content.map((task) => (
              <Link
                key={task.id}
                to={`/tasks/${task.id}`}
                className="flex flex-col gap-3 py-4 transition hover:bg-slate-50 sm:flex-row sm:items-center"
              >
                <div className="min-w-0 flex-1">
                  <p className="font-bold text-slate-800">{task.title}</p>
                  <p className="mt-1 text-xs text-slate-400">
                    {task.assignee?.displayName || 'Unassigned'} · Due {shortDate(task.dueDate)}
                  </p>
                </div>
                <span className={`badge ${statusTone[task.status]}`}>{titleCase(task.status)}</span>
              </Link>
            ))}
          </div>
        ) : (
          <EmptyState
            title="No tasks yet"
            description="Open this project on the task board and add its first deliverable."
          />
        )}
      </section>
    </>
  )
}
