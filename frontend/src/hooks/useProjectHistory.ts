import { useEffect, useState } from 'react'

import api from '@/api/axios'

export interface HistoryItem {
  id: number
  type: string
  message: string
  taskKey: string | null
  field: string | null
  oldValue: string | null
  newValue: string | null
  createdAt: string
  actor: { id: number; name: string; email: string } | null
}

// Učitava istoriju projekta (poziva se tek kad znamo da projekat postoji).
export function useProjectHistory(projectId: string, enabled: boolean) {
  const [items, setItems] = useState<HistoryItem[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [hasError, setHasError] = useState(false)

  useEffect(() => {
    if (!enabled) return

    let active = true

    api
      .get<HistoryItem[]>(`/projects/${projectId}/history`)
      .then((response) => {
        if (active) {
          setItems(response.data)
          setHasError(false)
        }
      })
      .catch(() => {
        if (active) setHasError(true)
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })

    return () => {
      active = false
    }
  }, [projectId, enabled])

  return { items, isLoading, hasError }
}