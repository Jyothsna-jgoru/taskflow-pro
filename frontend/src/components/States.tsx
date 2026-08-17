import { LoaderCircle } from 'lucide-react'
export function LoadingState({ label = 'Loading workspace' }: { label?: string }) {
  return (
    <div className="grid min-h-56 place-items-center">
      <div className="flex items-center gap-3 text-sm font-medium text-slate-500">
        <LoaderCircle className="animate-spin" size={20} />
        {label}
      </div>
    </div>
  )
}
export function EmptyState({
  title,
  description,
  action,
}: {
  title: string
  description: string
  action?: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/70 px-6 py-12 text-center">
      <h3 className="font-bold text-slate-800">{title}</h3>
      <p className="mx-auto mt-1 max-w-md text-sm text-slate-500">{description}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}
export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string
  title: string
  description?: string
  action?: React.ReactNode
}) {
  return (
    <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        {eyebrow && (
          <p className="mb-1 text-xs font-bold uppercase tracking-[.18em] text-indigo-600">{eyebrow}</p>
        )}
        <h1 className="text-2xl font-extrabold tracking-tight text-slate-950 sm:text-3xl">{title}</h1>
        {description && <p className="mt-1 max-w-2xl text-sm text-slate-500">{description}</p>}
      </div>
      {action}
    </div>
  )
}
