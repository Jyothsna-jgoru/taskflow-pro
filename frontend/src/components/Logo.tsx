export function Logo({ light = false }: { light?: boolean }) {
  return (
    <div className="flex items-center gap-3">
      <div className="grid size-9 place-items-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-sm font-black text-white shadow-lg shadow-indigo-500/20">
        TF
      </div>
      <div>
        <div className={`text-sm font-extrabold tracking-tight ${light ? 'text-white' : 'text-slate-900'}`}>
          TaskFlow Pro
        </div>
        <div
          className={`text-[10px] font-semibold uppercase tracking-[.16em] ${light ? 'text-slate-400' : 'text-slate-400'}`}
        >
          Productivity engine
        </div>
      </div>
    </div>
  )
}
