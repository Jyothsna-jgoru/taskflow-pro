import type { TaskPriority, TaskStatus } from '../types'
export const titleCase = (value: string) =>
  value
    .toLowerCase()
    .replaceAll('_', ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
export const shortDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric' }).format(new Date(`${value}T12:00:00`))
    : 'No date'
export const relativeTime = (value: string) => {
  const seconds = Math.round((new Date(value).getTime() - Date.now()) / 1000)
  const rtf = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })
  if (Math.abs(seconds) < 3600) return rtf.format(Math.round(seconds / 60), 'minute')
  if (Math.abs(seconds) < 86400) return rtf.format(Math.round(seconds / 3600), 'hour')
  return rtf.format(Math.round(seconds / 86400), 'day')
}
export const statusTone: Record<TaskStatus, string> = {
  TODO: 'bg-slate-100 text-slate-600',
  IN_PROGRESS: 'bg-blue-50 text-blue-700',
  BLOCKED: 'bg-rose-50 text-rose-700',
  IN_REVIEW: 'bg-amber-50 text-amber-700',
  DONE: 'bg-emerald-50 text-emerald-700',
}
export const priorityTone: Record<TaskPriority, string> = {
  LOW: 'text-slate-500',
  MEDIUM: 'text-blue-600',
  HIGH: 'text-amber-600',
  URGENT: 'text-rose-600',
}
