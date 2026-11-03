
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import axios from 'axios'

import api from '@/api/axios'

import { isLoggedIn, saveSession } from '@/lib/session'
import AuthCard from '@/components/ui/AuthCard'
import { btnPrimary, fieldClass, labelClass } from '@/components/ui/task-ui'

export default function Register() {
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  if (isLoggedIn()) {
    return <Navigate to="/projects" replace />
  }

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    const trimmedName = name.trim()
    const normalizedEmail = email.trim().toLowerCase()

    if (!trimmedName || !normalizedEmail || !password) {
      setError('Please fill in all fields.')
      return
    }

    if (password.length < 6) {
      setError('Password must contain at least 6 characters.')
      return
    }

    setIsLoading(true)

    try {
      // Šaljemo podatke za registraciju backendu.
      const response = await api.post('/auth/register', {
        name: trimmedName,
        email: normalizedEmail,
        password,
      })

      const { token, user } = response.data

      // Čuvamo sesiju nakon uspešne registracije.
      saveSession(token, {
        id: user.id,
        name: user.name,
        email: user.email,
      })

      // Preusmeravamo korisnika na projekte.
      navigate('/projects', { replace: true })
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const data = err.response?.data

        const firstFieldError = data?.errors
          ? (Object.values(data.errors)[0] as string | undefined)
          : undefined

        if (err.response?.status === 409) {
          setError('An account with this email already exists.')
        } else if (err.response?.status === 400) {
          setError(firstFieldError ?? data?.message ?? 'Invalid data.')
        } else if (!err.response) {
          setError(
            'Cannot connect to the server. Is the backend running?',
          )
        } else {
          setError(data?.message ?? 'Unable to create your account.')
        }
      } else {
        setError('An unexpected error occurred.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <AuthCard
      title="Create your account"
      subtitle="Sign up to start managing your projects."
      footer={
        <>
          Already have one?{' '}
          <Link
            to="/login"
            className="font-medium text-[#111827] hover:underline"
          >
            Log in
          </Link>
        </>
      }
    >
      <form onSubmit={handleRegister} className="space-y-4">
        <div>
          <label htmlFor="name" className={labelClass}>
            Name
          </label>

          <input
            id="name"
            type="text"
            autoComplete="name"
            required
            value={name}
            onChange={(event) => setName(event.target.value)}
            className={fieldClass}
            placeholder="Enter your name"
          />
        </div>

        <div>
          <label htmlFor="email" className={labelClass}>
            Email
          </label>

          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className={fieldClass}
            placeholder="you@example.com"
          />
        </div>

        <div>
          <label htmlFor="password" className={labelClass}>
            Password
          </label>

          <input
            id="password"
            type="password"
            autoComplete="new-password"
            required
            minLength={6}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="At least 6 characters"
            className={fieldClass}
          />
        </div>

        {error && (
          <p
            role="alert"
            className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
          >
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={isLoading}
          className={`${btnPrimary} h-10 w-full ${
            isLoading
              ? 'cursor-not-allowed opacity-50'
              : 'cursor-pointer opacity-100'
          }`}
        >
          {isLoading ? 'Creating account...' : 'Sign up'}
        </button>
      </form>
    </AuthCard>
  )
}
