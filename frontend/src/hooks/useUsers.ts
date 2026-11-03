import { useEffect, useState } from 'react'

import api from '@/api/axios'

export interface AppUser {
  id: number
  name: string
  email: string
}

// Ucitava sve korisnike (za Assignee dropdown i filtere).
export function useUsers(): AppUser[] {
  const [users, setUsers] = useState<AppUser[]>([])

  useEffect(() => {
    let active = true

    api
      .get<AppUser[]>('/users')
      .then((response) => {
        if (active) setUsers(response.data)
      })
      .catch(() => {
        // Lista korisnika nije kriticna, dropdown ce samo biti prazan.
      })

    return () => {
      active = false
    }
  }, [])

  return users
}