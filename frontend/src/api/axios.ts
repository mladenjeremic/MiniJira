import axios from 'axios'

import { clearSession, getToken } from '@/lib/session'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Uz svaki zahtev saljemo JWT token.
api.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

// Ako token istekne ili nije validan (401), korisnika vracamo na login.
api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const url = error.config?.url ?? ''
      const isAuthCall =
        url.includes('/auth/login') || url.includes('/auth/register')

      if (!isAuthCall) {
        clearSession()

        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
    }

    return Promise.reject(error)
  },
)

export default api