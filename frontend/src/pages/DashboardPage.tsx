import { useQuery } from '@tanstack/react-query'
import {
  AlertTriangle,
  ArrowUpRight,
  CalendarClock,
  CheckCircle2,
  CircleDot,
  ListChecks,
  TrendingUp,
} from 'lucide-react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { Link } from 'react-router-dom'
import { LoadingState, PageHeader } from '../components/States'
import { useWorkspace } from '../context/WorkspaceContext'
import { api } from '../lib/api'
import { relativeTime, titleCase } from '../lib/format'
import type { Dashboard } from '../types'

const colors = ['#94a3b8', '#3b82f6', '#f43f5e', '#f59e0b', '#10b981']
export function DashboardPage() {
  const { workspace } = useWorkspace()
  const query = useQuery({
    queryKey: ['dashboard', workspace?.id],
    enabled: Boolean(workspace),
    queryFn: async () => (await api.get<Dashboard>(`/workspaces/${workspace!.id}/dashboard`)).data,
  })
  if (!workspace) return <LoadingState label="Preparing your workspace" />
  if (query.isLoading) return <LoadingState label="Building your dashboard" />
  const data = query.data
  const statusData = Object.entries(data?.tasksByStatus ?? {}).map(([name, value]) => ({
    name: titleCase(name),
    value,
  }))
  const cards = [
    ['Total tasks', data?.totalTasks ?? 0, ListChecks, 'text-indigo-600', 'bg-indigo-50'],
    ['Overdue', data?.overdueTasks ?? 0, AlertTriangle, 'text-rose-600', 'bg-rose-50'],
    ['Due this week', data?.tasksDueThisWeek ?? 0, CalendarClock, 'text-amber-600', 'bg-amber-50'],
    ['Completion', `${data?.completionPercentage ?? 0}%`, TrendingUp, 'text-emerald-600', 'bg-emerald-50'],
  ] as const
  return (
    <>
      <PageHeader
        eyebrow="Workspace overview"
        title={`Good day, ${workspace.name}`}
        description="A live view of delivery health, current workload, and the work that needs attention."
        action={
          <Link to="/board" className="btn-primary">
            Open task board <ArrowUpRight size={17} />
          </Link>
        }
      />
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map(([label, value, Icon, tone, bg]) => (
          <article key={label} className="panel p-5">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-[.12em] text-slate-400">{label}</p>
                <p className="mt-3 text-3xl font-extrabold tracking-tight text-slate-950">{value}</p>
              </div>
              <div className={`grid size-10 place-items-center rounded-xl ${bg} ${tone}`}>
                <Icon size={20} />
              </div>
            </div>
            <p className="mt-4 text-xs font-medium text-slate-400">Updated from workspace activity</p>
          </article>
        ))}
      </section>
      <section className="mt-5 grid gap-5 xl:grid-cols-[1.15fr_.85fr]">
        <article className="panel p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-extrabold">Project progress</h2>
              <p className="mt-1 text-xs text-slate-400">Completed tasks across active projects</p>
            </div>
          </div>
          <div className="mt-6 h-72">
            {data?.projectProgress.length ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.projectProgress} margin={{ left: -18, right: 8 }}>
                  <CartesianGrid vertical={false} stroke="#eef2f7" />
                  <XAxis
                    dataKey="projectName"
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 11, fill: '#64748b' }}
                  />
                  <YAxis
                    domain={[0, 100]}
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 11, fill: '#94a3b8' }}
                  />
                  <Tooltip
                    cursor={{ fill: '#f8fafc' }}
                    contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }}
                  />
                  <Bar dataKey="percentage" fill="#6366f1" radius={[8, 8, 0, 0]} barSize={34} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="grid h-full place-items-center text-sm text-slate-400">
                No project metrics yet
              </div>
            )}
          </div>
        </article>
        <article className="panel p-6">
          <h2 className="font-extrabold">Tasks by status</h2>
          <p className="mt-1 text-xs text-slate-400">Distribution across the workflow</p>
          <div className="mt-3 grid items-center sm:grid-cols-[1fr_150px] xl:grid-cols-1 2xl:grid-cols-[1fr_150px]">
            <div className="h-52">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={statusData}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={58}
                    outerRadius={82}
                    paddingAngle={3}
                  >
                    {statusData.map((_, index) => (
                      <Cell key={index} fill={colors[index]} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="space-y-2">
              {statusData.map((item, index) => (
                <div key={item.name} className="flex items-center justify-between gap-5 text-xs">
                  <span className="flex items-center gap-2 font-medium text-slate-500">
                    <i className="size-2 rounded-full" style={{ backgroundColor: colors[index] }} />
                    {item.name}
                  </span>
                  <strong>{item.value}</strong>
                </div>
              ))}
            </div>
          </div>
        </article>
      </section>
      <section className="mt-5 grid gap-5 xl:grid-cols-[.85fr_1.15fr]">
        <article className="panel p-6">
          <h2 className="font-extrabold">Active workload</h2>
          <p className="mt-1 text-xs text-slate-400">Open assignments by teammate</p>
          <div className="mt-5 space-y-4">
            {data?.workload.map((row) => {
              const max = Math.max(...data.workload.map((w) => w.activeTasks), 1)
              return (
                <div key={row.userId}>
                  <div className="mb-2 flex justify-between text-xs">
                    <span className="font-bold text-slate-600">{row.displayName}</span>
                    <span className="font-semibold text-slate-400">{row.activeTasks} active</span>
                  </div>
                  <div className="h-2 rounded-full bg-slate-100">
                    <div
                      className="h-2 rounded-full bg-indigo-500"
                      style={{ width: `${Math.max(8, (row.activeTasks / max) * 100)}%` }}
                    />
                  </div>
                </div>
              )
            })}
            {!data?.workload.length && (
              <p className="py-8 text-center text-sm text-slate-400">No active assignments</p>
            )}
          </div>
        </article>
        <article className="panel p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-extrabold">Recent activity</h2>
              <p className="mt-1 text-xs text-slate-400">Meaningful changes across the workspace</p>
            </div>
            <CircleDot className="text-indigo-500" size={20} />
          </div>
          <div className="mt-5 space-y-1">
            {data?.recentActivity.map((event) => (
              <div key={event.id} className="flex gap-3 rounded-xl px-2 py-3 hover:bg-slate-50">
                <div className="grid size-8 shrink-0 place-items-center rounded-full bg-slate-100 text-slate-500">
                  <CheckCircle2 size={15} />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-slate-700">{event.message}</p>
                  <p className="mt-1 text-xs text-slate-400">
                    {relativeTime(event.createdAt)} · {event.actor.displayName}
                  </p>
                </div>
              </div>
            ))}
            {!data?.recentActivity.length && (
              <p className="py-8 text-center text-sm text-slate-400">No activity recorded yet</p>
            )}
          </div>
        </article>
      </section>
    </>
  )
}
