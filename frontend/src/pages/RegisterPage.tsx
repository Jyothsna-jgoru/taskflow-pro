import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useQuery } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRight, LoaderCircle } from 'lucide-react'
import { z } from 'zod'
import { AuthShell } from '../components/AuthShell'
import { useAuth } from '../context/AuthContext'
import { api, errorMessage } from '../lib/api'
import type { InvitationPreview } from '../types'
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
  const [searchParams] = useSearchParams()
  const [error, setError] = useState('')
  const inviteToken = searchParams.get('invite')?.trim() || undefined
  const invitation = useQuery({
    queryKey: ['invitation-preview', inviteToken],
    enabled: Boolean(inviteToken),
    retry: false,
    queryFn: async () => (await api.get<InvitationPreview>(`/invitations/${inviteToken}`)).data,
  })
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { email: '' } })
  useEffect(() => {
    if (invitation.data) setValue('email', invitation.data.email)
  }, [invitation.data, setValue])
  if (user) return <Navigate to="/" replace />
  const submit = handleSubmit(async (values) => {
    try {
      setError('')
      if (inviteToken && !invitation.data) return
      await signUp(values.displayName, values.email, values.password, inviteToken)
      navigate('/select-workspace')
    } catch (e) {
      setError(errorMessage(e))
    }
  })
  return (
    <AuthShell
      title={invitation.data ? `Join ${invitation.data.workspaceName}` : 'Create your account'}
      description={
        invitation.data
          ? 'Your invitation has selected the correct email address for you.'
          : 'Start a focused workspace for your team in under a minute.'
      }
    >
      <form onSubmit={submit} className="space-y-5">
        {(error || invitation.isError) && (
          <div
            role="alert"
            className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
          >
            {error || errorMessage(invitation.error)}
          </div>
        )}
        {inviteToken && invitation.isLoading && (
          <div className="flex items-center gap-2 rounded-xl border border-indigo-100 bg-indigo-50 px-4 py-3 text-sm text-indigo-700">
            <LoaderCircle className="animate-spin" size={16} /> Checking your invitation…
          </div>
        )}
        {invitation.data && (
          <div className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
            You are joining <strong>{invitation.data.workspaceName}</strong> as an invited teammate.
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
            readOnly={Boolean(invitation.data)}
            {...register('email')}
          />
          {errors.email && <p className="mt-1 text-xs text-rose-600">{errors.email.message}</p>}
        </div>
        <div>
          <label className="label">Password</label>
          <input className="input" type="password" autoComplete="new-password" {...register('password')} />
          {errors.password && <p className="mt-1 text-xs text-rose-600">{errors.password.message}</p>}
        </div>
        <button
          className="btn-primary w-full"
          disabled={isSubmitting || Boolean(inviteToken && !invitation.data)}
        >
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
