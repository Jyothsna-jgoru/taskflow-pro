import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRight, LoaderCircle } from 'lucide-react'
import { z } from 'zod'
import { AuthShell } from '../components/AuthShell'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../lib/api'

const schema = z.object({
  email: z.email('Enter a valid email'),
  password: z.string().min(8, 'Password must contain at least 8 characters'),
})
type Values = z.infer<typeof schema>
export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { email: 'admin@taskflow.local', password: 'Admin123!' },
  })
  if (user) return <Navigate to="/" replace />
  const submit = handleSubmit(async (values) => {
    try {
      setError('')
      await login(values.email, values.password)
      navigate('/')
    } catch (e) {
      setError(errorMessage(e))
    }
  })
  return (
    <AuthShell title="Welcome back" description="Sign in to see what your team is shipping today.">
      <form onSubmit={submit} className="space-y-5">
        {error && (
          <div
            role="alert"
            className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700"
          >
            {error}
          </div>
        )}
        <div>
          <label className="label">Email address</label>
          <input className="input" type="email" autoComplete="email" {...register('email')} />
          {errors.email && <p className="mt-1 text-xs text-rose-600">{errors.email.message}</p>}
        </div>
        <div>
          <div className="flex justify-between">
            <label className="label">Password</label>
            <span className="text-xs font-semibold text-slate-400">Demo credentials filled in</span>
          </div>
          <input
            className="input"
            type="password"
            autoComplete="current-password"
            {...register('password')}
          />
          {errors.password && <p className="mt-1 text-xs text-rose-600">{errors.password.message}</p>}
        </div>
        <button className="btn-primary w-full" disabled={isSubmitting}>
          {isSubmitting ? (
            <LoaderCircle className="animate-spin" size={18} />
          ) : (
            <>
              Sign in <ArrowRight size={17} />
            </>
          )}
        </button>
      </form>
      <p className="mt-7 text-center text-sm text-slate-500">
        New to TaskFlow?{' '}
        <Link to="/register" className="font-bold text-indigo-600 hover:text-indigo-700">
          Create an account
        </Link>
      </p>
    </AuthShell>
  )
}
