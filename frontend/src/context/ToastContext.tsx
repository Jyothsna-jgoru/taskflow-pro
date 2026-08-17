import { createContext, useContext, useState, type ReactNode } from 'react'
import { CheckCircle2, X, XCircle } from 'lucide-react'
type Kind = 'success' | 'error'
interface Toast {
  id: number
  message: string
  kind: Kind
}
const ToastContext = createContext<{ show: (message: string, kind?: Kind) => void } | null>(null)
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const show = (message: string, kind: Kind = 'success') => {
    const id = Date.now()
    setToasts((v) => [...v, { id, message, kind }])
    window.setTimeout(() => setToasts((v) => v.filter((t) => t.id !== id)), 4000)
  }
  return (
    <ToastContext.Provider value={{ show }}>
      {children}
      <div className="fixed right-4 top-4 z-50 flex w-[calc(100%-2rem)] max-w-sm flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-panel"
          >
            <span className={t.kind === 'success' ? 'text-emerald-600' : 'text-rose-600'}>
              {t.kind === 'success' ? <CheckCircle2 size={20} /> : <XCircle size={20} />}
            </span>
            <p className="flex-1 text-sm font-medium text-slate-700">{t.message}</p>
            <button aria-label="Dismiss" onClick={() => setToasts((v) => v.filter((x) => x.id !== t.id))}>
              <X size={16} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
export const useToast = () => {
  const value = useContext(ToastContext)
  if (!value) throw new Error('useToast requires ToastProvider')
  return value
}
