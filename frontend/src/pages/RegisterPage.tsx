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
  displayName: z.string().min(2, 'Enter your name').max(100),
  email: z.email('Enter a valid email'),
  password: z
    .string()
    .min(8, 'Use at least 8 characters')
    .max(72)
    .regex(/[A-Z]/, 'Add an uppercase letter')
    .regex(/[0-9]/, 'Add a number'),
})
type Values = z.infer<typeof schema>
export function RegisterPage() {
  const { user, register: signUp } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema) })
  if (user) return <Navigate to="/" replace />
  const submit = handleSubmit(async (values) => {
    try {
      setError('')
      await signUp(values.displayName, values.email, values.password)
      navigate('/select-workspace')
    } catch (e) {
      setError(errorMessage(e))
    }
  })
  return (
    <AuthShell
      title="Create your account"
      description="Start a focused workspace for your team in under a minute."
    >
      <form onSubmit={submit} className="space-y-5">
        {error && (
          <div
            role="alert"
            className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
          >
            {error}
          </div>
        )}
        <div>
          <label className="label">Full name</label>
          <input
            className="input"
            autoComplete="name"
            placeholder="Jordan Rivera"
            {...register('displayName')}
          />
          {errors.displayName && <p className="mt-1 text-xs text-rose-600">{errors.displayName.message}</p>}
        </div>
        <div>
          <label className="label">Work email</label>
          <input
            className="input"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            {...register('email')}
          />
          {errors.email && <p className="mt-1 text-xs text-rose-600">{errors.email.message}</p>}
        </div>
        <div>
          <label className="label">Password</label>
          <input className="input" type="password" autoComplete="new-password" {...register('password')} />
          {errors.password && <p className="mt-1 text-xs text-rose-600">{errors.password.message}</p>}
        </div>
        <button className="btn-primary w-full" disabled={isSubmitting}>
          {isSubmitting ? (
            <LoaderCircle className="animate-spin" size={18} />
          ) : (
            <>
              Create account <ArrowRight size={17} />
            </>
          )}
        </button>
      </form>
      <p className="mt-7 text-center text-sm text-slate-500">
        Already have an account?{' '}
        <Link to="/login" className="font-bold text-indigo-600">
          Sign in
        </Link>
      </p>
    </AuthShell>
  )
}
