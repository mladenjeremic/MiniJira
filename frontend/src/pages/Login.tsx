import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import axios from 'axios'

import api from '@/api/axios'

import { isLoggedIn, saveSession } from '@/lib/session'
import AuthCard from '@/components/ui/AuthCard'
import { btnPrimary, fieldClass, labelClass } from '@/components/ui/task-ui'

export default function Login() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  // Vec ulogovan korisnik ne treba da vidi login stranicu.
  if (isLoggedIn()) {
    return <Navigate to="/projects" replace />
  }

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    const normalizedEmail = email.trim().toLowerCase()

    if (!normalizedEmail || !password) {
      setError('Please enter your email and password.')
      return
    }

    setIsLoading(true)

    try {
      const response = await api.post('/auth/login', {
        email: normalizedEmail,
        password,
      })

      const { token, user } = response.data

      saveSession(token, {
        id: user.id,
        name: user.name,
        email: user.email,
      })

      navigate('/projects', { replace: true })
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 401) {
          setError('Invalid email or password.')
        } else if (!err.response) {
          setError('Cannot connect to the server. Is the backend running?')
        } else {
          setError(err.response.data?.message ?? 'Unable to log in.')
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
      title="Log in to MiniJira"
      subtitle="Enter your email and password to continue."
      footer={
        <>
          No account?{' '}
          <Link to="/register" className="font-medium text-[#111827] hover:underline">
            Create one
          </Link>
        </>
      }
    >
      <form onSubmit={handleLogin} className="space-y-4">
        <div>
          <label htmlFor="email" className={labelClass}>
            Email
          </label>

          <input
            id="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@example.com"
            className={fieldClass}
          />
        </div>

        <div>
          <label htmlFor="password" className={labelClass}>
            Password
          </label>

          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
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
          className={`${btnPrimary} h-10 w-full`}
        >
          {isLoading ? 'Logging in...' : 'Log in'}
        </button>
      </form>
    </AuthCard>
  )
}
