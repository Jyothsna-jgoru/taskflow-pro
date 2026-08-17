import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarDays, Flag, Plus, Search, SlidersHorizontal, X } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { EmptyState, LoadingState, PageHeader } from '../components/States'
import { useWorkspace } from '../context/WorkspaceContext'
import { useToast } from '../context/ToastContext'
import { api, errorMessage } from '../lib/api'
import { priorityTone, shortDate, titleCase } from '../lib/format'
import type { Member, Project, Task, TaskPage, TaskPriority, TaskStatus } from '../types'

const columns: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'IN_REVIEW', 'DONE']
const columnTone: Record<TaskStatus, string> = {
  TODO: 'bg-slate-400',
  IN_PROGRESS: 'bg-blue-500',
  BLOCKED: 'bg-rose-500',
  IN_REVIEW: 'bg-amber-500',
  DONE: 'bg-emerald-500',
}
export function TaskBoardPage() {
  const { workspace } = useWorkspace()
  const { show } = useToast()
  const client = useQueryClient()
  const [params] = useSearchParams()
  const [search, setSearch] = useState('')
  const [priority, setPriority] = useState('')
  const [projectId, setProjectId] = useState(params.get('projectId') ?? '')
  const [createOpen, setCreateOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [createProject, setCreateProject] = useState(projectId)
  const [createPriority, setCreatePriority] = useState<TaskPriority>('MEDIUM')
  const [assigneeId, setAssigneeId] = useState('')
  const [dueDate, setDueDate] = useState('')
  const tasks = useQuery({
    queryKey: ['tasks', workspace?.id, search, priority, projectId],
    enabled: Boolean(workspace),
    queryFn: async () =>
      (
        await api.get<TaskPage>(`/workspaces/${workspace!.id}/tasks`, {
          params: {
            search: search || undefined,
            priority: priority || undefined,
            projectId: projectId || undefined,
            size: 100,
            sort: 'updatedAt',
          },
        })
      ).data,
  })
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
        await api.post<Task>(`/workspaces/${workspace!.id}/tasks`, {
          projectId: createProject,
          title,
          description,
          status: 'TODO',
          priority: createPriority,
          assigneeId: assigneeId || null,
          dueDate: dueDate || null,
          labels: [],
        })
      ).data,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['tasks'] })
      client.invalidateQueries({ queryKey: ['dashboard'] })
      show('Task created')
      setCreateOpen(false)
      setTitle('')
      setDescription('')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const move = useMutation({
    mutationFn: async ({ task, status }: { task: Task; status: TaskStatus }) =>
      api.put(`/workspaces/${workspace!.id}/tasks/${task.id}`, {
        projectId: task.projectId,
        title: task.title,
        description: task.description ?? '',
        status,
        priority: task.priority,
        assigneeId: task.assignee?.id ?? null,
        dueDate: task.dueDate ?? null,
        labels: task.labels,
        version: task.version,
      }),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['tasks'] })
      client.invalidateQueries({ queryKey: ['dashboard'] })
      show('Task status updated')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const grouped = useMemo(
    () =>
      Object.fromEntries(
        columns.map((status) => [
          status,
          (tasks.data?.content ?? []).filter((task) => task.status === status),
        ]),
      ) as Record<TaskStatus, Task[]>,
    [tasks.data],
  )
  if (tasks.isLoading) return <LoadingState label="Loading task board" />
  return (
    <>
      <PageHeader
        eyebrow="Delivery flow"
        title="Task board"
        description="Search, prioritize, and move work through a shared team workflow."
        action={
          <button
            onClick={() => {
              setCreateProject(projectId || projects.data?.[0]?.id || '')
              setCreateOpen(true)
            }}
            className="btn-primary"
          >
            <Plus size={17} />
            New task
          </button>
        }
      />
      <div className="panel mb-5 flex flex-col gap-3 p-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-3 text-slate-400" size={17} />
          <input
            className="input pl-9"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search tasks and descriptions…"
          />
        </div>
        <div className="relative sm:w-52">
          <SlidersHorizontal className="absolute left-3 top-3 text-slate-400" size={16} />
          <select
            className="input appearance-none pl-9"
            value={projectId}
            onChange={(e) => setProjectId(e.target.value)}
          >
            <option value="">All projects</option>
            {projects.data
              ?.filter((p) => p.status !== 'ARCHIVED')
              .map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
          </select>
        </div>
        <select className="input sm:w-40" value={priority} onChange={(e) => setPriority(e.target.value)}>
          <option value="">All priorities</option>
          {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((p) => (
            <option key={p}>{titleCase(p)}</option>
          ))}
        </select>
      </div>
      <div className="grid min-w-max grid-cols-5 gap-4 overflow-x-auto pb-4">
        {columns.map((status) => (
          <section key={status} className="w-[285px] rounded-2xl bg-slate-100/70 p-3">
            <header className="mb-3 flex items-center justify-between px-1">
              <div className="flex items-center gap-2">
                <i className={`size-2 rounded-full ${columnTone[status]}`} />
                <h2 className="text-xs font-extrabold uppercase tracking-[.1em] text-slate-600">
                  {titleCase(status)}
                </h2>
              </div>
              <span className="rounded-full bg-white px-2 py-0.5 text-xs font-bold text-slate-400">
                {grouped[status].length}
              </span>
            </header>
            <div className="space-y-3">
              {grouped[status].map((task) => (
                <article
                  key={task.id}
                  className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-panel"
                >
                  <div className="mb-3 flex items-start justify-between gap-2">
                    <span
                      className={`flex items-center gap-1 text-[10px] font-extrabold uppercase tracking-wider ${priorityTone[task.priority]}`}
                    >
                      <Flag size={12} />
                      {task.priority}
                    </span>
                    <select
                      aria-label={`Move ${task.title}`}
                      value={task.status}
                      disabled={move.isPending}
                      onChange={(e) => move.mutate({ task, status: e.target.value as TaskStatus })}
                      className="max-w-24 rounded-md border-0 bg-slate-50 px-1 py-0.5 text-[9px] font-bold text-slate-500 outline-none"
                    >
                      {columns.map((s) => (
                        <option key={s} value={s}>
                          {titleCase(s)}
                        </option>
                      ))}
                    </select>
                  </div>
                  <Link
                    to={`/tasks/${task.id}`}
                    className="block font-bold leading-snug text-slate-800 hover:text-indigo-600"
                  >
                    {task.title}
                  </Link>
                  <p className="mt-2 line-clamp-2 text-xs leading-relaxed text-slate-400">
                    {task.description || 'No description'}
                  </p>
                  <div className="mt-4 flex flex-wrap gap-1">
                    {task.labels.slice(0, 3).map((label) => (
                      <span
                        key={label}
                        className="rounded-md bg-indigo-50 px-2 py-1 text-[10px] font-bold text-indigo-600"
                      >
                        {label}
                      </span>
                    ))}
                  </div>
                  <footer className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3">
                    <div className="flex items-center gap-1 text-[10px] font-semibold text-slate-400">
                      <CalendarDays size={13} />
                      {shortDate(task.dueDate)}
                    </div>
                    <div
                      title={task.assignee?.displayName || 'Unassigned'}
                      className="grid size-7 place-items-center rounded-full bg-slate-900 text-[9px] font-extrabold text-white"
                    >
                      {task.assignee?.displayName
                        .split(' ')
                        .map((x) => x[0])
                        .slice(0, 2)
                        .join('') || '—'}
                    </div>
                  </footer>
                </article>
              ))}
              {!grouped[status].length && (
                <div className="rounded-xl border border-dashed border-slate-300 px-3 py-8 text-center text-xs font-medium text-slate-400">
                  No tasks
                </div>
              )}
            </div>
          </section>
        ))}
      </div>
      {!tasks.data?.totalElements && (
        <EmptyState
          title="Your board is clear"
          description="Create a task and it will appear in To Do, ready for your team."
        />
      )}
      {createOpen && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/45 p-4">
          <div className="panel w-full max-w-xl p-6">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-xl font-extrabold">Create a task</h2>
                <p className="mt-1 text-sm text-slate-500">Add a concrete, assignable piece of work.</p>
              </div>
              <button onClick={() => setCreateOpen(false)} className="text-slate-400">
                <X />
              </button>
            </div>
            <div className="mt-6 space-y-4">
              <div>
                <label className="label">Title</label>
                <input
                  autoFocus
                  className="input"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="What needs to be done?"
                />
              </div>
              <div>
                <label className="label">Description</label>
                <textarea
                  className="input min-h-24 resize-none"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="label">Project</label>
                  <select
                    className="input"
                    value={createProject}
                    onChange={(e) => setCreateProject(e.target.value)}
                  >
                    <option value="">Select a project</option>
                    {projects.data
                      ?.filter((p) => p.status !== 'ARCHIVED')
                      .map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name}
                        </option>
                      ))}
                  </select>
                </div>
                <div>
                  <label className="label">Assignee</label>
                  <select
                    className="input"
                    value={assigneeId}
                    onChange={(e) => setAssigneeId(e.target.value)}
                  >
                    <option value="">Unassigned</option>
                    {members.data?.map((m) => (
                      <option key={m.userId} value={m.userId}>
                        {m.displayName}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label">Priority</label>
                  <select
                    className="input"
                    value={createPriority}
                    onChange={(e) => setCreatePriority(e.target.value as TaskPriority)}
                  >
                    {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((p) => (
                      <option key={p}>{p}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label">Due date</label>
                  <input
                    className="input"
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                  />
                </div>
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button className="btn-secondary" onClick={() => setCreateOpen(false)}>
                  Cancel
                </button>
                <button
                  className="btn-primary"
                  disabled={!title.trim() || !createProject || create.isPending}
                  onClick={() => create.mutate()}
                >
                  Create task
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
