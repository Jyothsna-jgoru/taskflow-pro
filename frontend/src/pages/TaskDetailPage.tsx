import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  Clock3,
  MessageSquare,
  Send,
  Tag,
  Trash2,
  UserRound,
} from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { EmptyState, LoadingState } from '../components/States'
import { useAuth } from '../context/AuthContext'
import { useWorkspace } from '../context/WorkspaceContext'
import { useToast } from '../context/ToastContext'
import { api, errorMessage } from '../lib/api'
import { relativeTime, shortDate, statusTone, titleCase } from '../lib/format'
import type { Member, Project, TaskDetail, TaskPriority, TaskStatus } from '../types'
const statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'IN_REVIEW', 'DONE']
export function TaskDetailPage() {
  const { taskId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { workspace } = useWorkspace()
  const { show } = useToast()
  const client = useQueryClient()
  const [comment, setComment] = useState('')
  const [editing, setEditing] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState<TaskStatus>('TODO')
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM')
  const [projectId, setProjectId] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [labels, setLabels] = useState('')
  const detail = useQuery({
    queryKey: ['task', workspace?.id, taskId],
    enabled: Boolean(workspace && taskId),
    queryFn: async () => (await api.get<TaskDetail>(`/workspaces/${workspace!.id}/tasks/${taskId}`)).data,
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
  useEffect(() => {
    const t = detail.data?.task
    if (t) {
      setTitle(t.title)
      setDescription(t.description ?? '')
      setStatus(t.status)
      setPriority(t.priority)
      setProjectId(t.projectId)
      setAssigneeId(t.assignee?.id ?? '')
      setDueDate(t.dueDate ?? '')
      setLabels(t.labels.join(', '))
    }
  }, [detail.data])
  const save = useMutation({
    mutationFn: async () =>
      api.put(`/workspaces/${workspace!.id}/tasks/${taskId}`, {
        projectId,
        title,
        description,
        status,
        priority,
        assigneeId: assigneeId || null,
        dueDate: dueDate || null,
        labels: labels
          .split(',')
          .map((label) => label.trim())
          .filter(Boolean)
          .slice(0, 10),
        version: detail.data!.task.version,
      }),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['task', workspace?.id, taskId] })
      client.invalidateQueries({ queryKey: ['tasks'] })
      client.invalidateQueries({ queryKey: ['dashboard'] })
      show('Task updated')
      setEditing(false)
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const addComment = useMutation({
    mutationFn: async () =>
      api.post(`/workspaces/${workspace!.id}/tasks/${taskId}/comments`, { body: comment }),
    onSuccess: () => {
      setComment('')
      client.invalidateQueries({ queryKey: ['task', workspace?.id, taskId] })
      show('Comment added')
    },
    onError: (e) => show(errorMessage(e), 'error'),
  })
  const remove = useMutation({
    mutationFn: async () => api.delete(`/workspaces/${workspace!.id}/tasks/${taskId}`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['tasks'] })
      client.invalidateQueries({ queryKey: ['dashboard'] })
      show('Task deleted')
      navigate('/board')
    },
    onError: (error) => show(errorMessage(error), 'error'),
  })
  if (detail.isLoading) return <LoadingState label="Loading task" />
  if (!detail.data)
    return (
      <EmptyState
        title="Task not found"
        description="This task may have been removed or belongs to another workspace."
      />
    )
  const { task, comments, activity } = detail.data
  const canDelete = workspace?.currentUserRole !== 'MEMBER' || task.reporter.id === user?.id
  return (
    <>
      <Link
        to="/board"
        className="mb-5 inline-flex items-center gap-2 text-sm font-bold text-slate-500 hover:text-indigo-600"
      >
        <ArrowLeft size={16} />
        Task board
      </Link>
      <div className="grid gap-5 xl:grid-cols-[1fr_370px]">
        <main className="panel p-6 sm:p-8">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`badge ${statusTone[task.status]}`}>{titleCase(task.status)}</span>
            <span className="badge bg-slate-100 text-slate-600">{task.priority} priority</span>
            <span className="text-xs font-semibold text-slate-400">{task.projectName}</span>
          </div>
          {editing ? (
            <div className="mt-6 space-y-4">
              <div>
                <label className="label">Task title</label>
                <input
                  className="input text-lg font-bold"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>
              <div>
                <label className="label">Description</label>
                <textarea
                  className="input min-h-40 resize-y"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="label">Status</label>
                  <select
                    className="input"
                    value={status}
                    onChange={(e) => setStatus(e.target.value as TaskStatus)}
                  >
                    {statuses.map((s) => (
                      <option key={s} value={s}>
                        {titleCase(s)}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label">Priority</label>
                  <select
                    className="input"
                    value={priority}
                    onChange={(e) => setPriority(e.target.value as TaskPriority)}
                  >
                    {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((p) => (
                      <option key={p}>{p}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label">Project</label>
                  <select className="input" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
                    {projects.data?.map((p) => (
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
                  <label className="label">Due date</label>
                  <input
                    className="input"
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                  />
                </div>
                <div>
                  <label className="label">Labels</label>
                  <div className="relative">
                    <Tag className="absolute left-3 top-3 text-slate-400" size={16} />
                    <input
                      className="input pl-9"
                      value={labels}
                      onChange={(event) => setLabels(event.target.value)}
                      placeholder="backend, security"
                    />
                  </div>
                </div>
              </div>
              <div className="flex gap-3">
                <button
                  onClick={() => save.mutate()}
                  disabled={!title.trim() || save.isPending}
                  className="btn-primary"
                >
                  Save changes
                </button>
                <button onClick={() => setEditing(false)} className="btn-secondary">
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            <>
              <div className="mt-6 flex items-start justify-between gap-4">
                <div>
                  <h1 className="text-3xl font-extrabold tracking-tight text-slate-950">{task.title}</h1>
                  <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-slate-600">
                    {task.description || 'No description has been added.'}
                  </p>
                </div>
                <div className="flex shrink-0 gap-2">
                  {canDelete && (
                    <button
                      aria-label="Delete task"
                      title="Delete task"
                      onClick={() => {
                        if (window.confirm('Delete this task and its comments?')) remove.mutate()
                      }}
                      disabled={remove.isPending}
                      className="btn-secondary text-rose-600"
                    >
                      <Trash2 size={16} />
                    </button>
                  )}
                  <button onClick={() => setEditing(true)} className="btn-secondary">
                    Edit task
                  </button>
                </div>
              </div>
              <div className="mt-8 grid gap-3 border-y border-slate-100 py-5 sm:grid-cols-3">
                <div className="flex items-center gap-3">
                  <UserRound size={18} className="text-indigo-500" />
                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Assignee</p>
                    <p className="text-sm font-bold">{task.assignee?.displayName || 'Unassigned'}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <CalendarDays size={18} className="text-amber-500" />
                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Due date</p>
                    <p className="text-sm font-bold">{shortDate(task.dueDate)}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Clock3 size={18} className="text-slate-400" />
                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Updated</p>
                    <p className="text-sm font-bold">{relativeTime(task.updatedAt)}</p>
                  </div>
                </div>
              </div>
            </>
          )}
          <section className="mt-8">
            <div className="flex items-center gap-2">
              <MessageSquare size={19} className="text-indigo-500" />
              <h2 className="font-extrabold">Discussion</h2>
              <span className="text-xs font-bold text-slate-400">{comments.length}</span>
            </div>
            <div className="mt-5 space-y-5">
              {comments.map((entry) => (
                <article key={entry.id} className="flex gap-3">
                  <div className="grid size-9 shrink-0 place-items-center rounded-full bg-indigo-50 text-xs font-extrabold text-indigo-700">
                    {entry.author.displayName[0]}
                  </div>
                  <div className="flex-1 rounded-2xl rounded-tl-sm bg-slate-50 p-4">
                    <div className="flex justify-between gap-4">
                      <p className="text-xs font-extrabold text-slate-700">{entry.author.displayName}</p>
                      <time className="text-[11px] text-slate-400">{relativeTime(entry.createdAt)}</time>
                    </div>
                    <p className="mt-2 text-sm leading-relaxed text-slate-600">{entry.body}</p>
                  </div>
                </article>
              ))}
            </div>
            <div className="mt-6 flex gap-3">
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                className="input min-h-20 flex-1 resize-none"
                placeholder="Add a helpful comment…"
              />
              <button
                aria-label="Send comment"
                disabled={!comment.trim() || addComment.isPending}
                onClick={() => addComment.mutate()}
                className="btn-primary self-end"
              >
                <Send size={17} />
              </button>
            </div>
          </section>
        </main>
        <aside className="panel h-fit p-6">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="text-indigo-500" size={19} />
            <h2 className="font-extrabold">Activity</h2>
          </div>
          <div className="relative mt-6 space-y-6 before:absolute before:bottom-2 before:left-[5px] before:top-2 before:w-px before:bg-slate-200">
            {activity.map((event) => (
              <div key={event.id} className="relative flex gap-4">
                <i className="relative z-10 mt-1 size-[11px] shrink-0 rounded-full border-2 border-white bg-indigo-500 ring-2 ring-indigo-100" />
                <div>
                  <p className="text-sm font-semibold leading-relaxed text-slate-700">{event.message}</p>
                  <p className="mt-1 text-xs text-slate-400">{relativeTime(event.createdAt)}</p>
                </div>
              </div>
            ))}
            {!activity.length && <p className="text-sm text-slate-400">No activity yet.</p>}
          </div>
        </aside>
      </div>
    </>
  )
}
