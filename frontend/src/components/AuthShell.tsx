import type { ReactNode } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { Logo } from './Logo'
export function AuthShell({
  children,
  title,
  description,
}: {
  children: ReactNode
  title: string
  description: string
}) {
  return (
    <div className="grid min-h-screen bg-white lg:grid-cols-[1.05fr_.95fr]">
      <section className="relative hidden overflow-hidden bg-[#111827] p-12 lg:flex lg:flex-col lg:justify-between">
        <div className="absolute -right-32 -top-28 size-96 rounded-full bg-indigo-600/20 blur-3xl" />
        <Logo light />
        <div className="relative max-w-xl">
          <p className="mb-4 text-xs font-bold uppercase tracking-[.22em] text-indigo-300">
            Move work forward
          </p>
          <h2 className="text-5xl font-extrabold leading-[1.08] tracking-tight text-white">
            Clarity for every task.
            <br />
            Momentum for every team.
          </h2>
          <p className="mt-6 max-w-lg text-lg leading-relaxed text-slate-400">
            Plan projects, balance workload, and turn team activity into decisions—all from one focused
            workspace.
          </p>
          <div className="mt-10 grid grid-cols-3 gap-3">
            {['Workspace RBAC', 'Live dashboards', 'Audit history'].map((item) => (
              <div
                key={item}
                className="rounded-2xl border border-slate-700 bg-slate-800/50 p-4 text-sm font-semibold text-slate-200"
              >
                <CheckCircle2 className="mb-3 text-emerald-400" size={19} />
                {item}
              </div>
            ))}
          </div>
        </div>
        <p className="relative text-xs text-slate-500">Open-source local stack · No paid services required</p>
      </section>
      <section className="flex min-h-screen items-center justify-center px-6 py-12">
        <div className="w-full max-w-md">
          <div className="mb-10 lg:hidden">
            <Logo />
          </div>
          <p className="text-xs font-bold uppercase tracking-[.18em] text-indigo-600">TaskFlow Pro</p>
          <h1 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-950">{title}</h1>
          <p className="mt-2 text-sm leading-relaxed text-slate-500">{description}</p>
          <div className="mt-8">{children}</div>
        </div>
      </section>
    </div>
  )
}
