import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  BarChart3,
  BriefcaseBusiness,
  CheckSquare2,
  ChevronDown,
  LogOut,
  Menu,
  Settings,
  Users,
  X,
} from 'lucide-react'
import { Logo } from './Logo'
import { useAuth } from '../context/AuthContext'
import { useWorkspace } from '../context/WorkspaceContext'

const links = [
  ['/', 'Overview', BarChart3],
  ['/projects', 'Projects', BriefcaseBusiness],
  ['/board', 'Task board', CheckSquare2],
  ['/team', 'Team', Users],
  ['/settings', 'Settings', Settings],
] as const
export function Layout() {
  const [open, setOpen] = useState(false)
  const { user, logout } = useAuth()
  const { workspace, workspaces, select } = useWorkspace()
  const navigate = useNavigate()
  const signOut = () => {
    logout()
    navigate('/login')
  }
  return (
    <div className="min-h-screen bg-[#f7f8fb] text-slate-900">
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 bg-[#111827] px-4 py-5 transition-transform lg:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <div className="flex items-center justify-between px-2">
          <Logo light />
          <button className="text-slate-400 lg:hidden" onClick={() => setOpen(false)}>
            <X />
          </button>
        </div>
        <div className="mt-8">
          <label className="mb-2 block px-3 text-[10px] font-bold uppercase tracking-[.18em] text-slate-500">
            Workspace
          </label>
          <div className="relative">
            <select
              aria-label="Workspace"
              value={workspace?.id ?? ''}
              onChange={(e) => select(e.target.value)}
              className="w-full appearance-none rounded-xl border border-slate-700 bg-slate-800 px-3 py-3 pr-8 text-sm font-semibold text-slate-100 outline-none focus:border-indigo-500"
            >
              {workspaces.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name}
                </option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute right-3 top-3.5 text-slate-400" size={16} />
          </div>
        </div>
        <nav className="mt-7 space-y-1">
          {links.map(([to, label, Icon]) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition ${isActive ? 'bg-indigo-500 text-white shadow-lg shadow-indigo-950/30' : 'text-slate-400 hover:bg-slate-800 hover:text-white'}`
              }
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="absolute bottom-5 left-4 right-4 rounded-2xl border border-slate-700 bg-slate-800/70 p-3">
          <div className="flex items-center gap-3">
            <div className="grid size-9 place-items-center rounded-full bg-indigo-100 text-xs font-extrabold text-indigo-700">
              {user?.displayName
                .split(' ')
                .map((x) => x[0])
                .slice(0, 2)
                .join('')}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold text-white">{user?.displayName}</p>
              <p className="truncate text-xs text-slate-400">{workspace?.currentUserRole}</p>
            </div>
            <button aria-label="Sign out" onClick={signOut} className="text-slate-400 hover:text-white">
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>
      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200/80 bg-white/90 px-4 backdrop-blur lg:px-8">
          <button className="text-slate-600 lg:hidden" onClick={() => setOpen(true)}>
            <Menu />
          </button>
          <div className="hidden items-center gap-2 text-xs font-semibold text-slate-400 lg:flex">
            <span className="size-2 rounded-full bg-emerald-500" /> Systems operational
          </div>
          <div className="flex items-center gap-3">
            <div className="text-right">
              <p className="text-xs font-bold text-slate-700">{user?.displayName}</p>
              <p className="text-[11px] text-slate-400">{user?.email}</p>
            </div>
            <div className="grid size-9 place-items-center rounded-full bg-slate-900 text-xs font-extrabold text-white">
              {user?.displayName[0]}
            </div>
          </div>
        </header>
        <main className="mx-auto max-w-[1500px] px-4 py-7 sm:px-6 lg:px-8">
          <Outlet />
        </main>
      </div>
      {open && (
        <button
          aria-label="Close menu overlay"
          onClick={() => setOpen(false)}
          className="fixed inset-0 z-30 bg-slate-950/40 lg:hidden"
        />
      )}
    </div>
  )
}
