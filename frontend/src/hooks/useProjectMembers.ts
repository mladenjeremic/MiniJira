import { useMemo } from 'react'

import type { Project } from '@/context/ProjectsContext'
import type { AppUser } from '@/hooks/useUsers'

// Vlasnik + učesnici projekta (bez duplikata).
export function useProjectMembers(project: Project | undefined): AppUser[] {
  return useMemo(() => {
    if (!project) return []

    const byId = new Map<number, AppUser>()

    if (project.owner) {
      byId.set(project.owner.id, project.owner)
    }

    project.participants.forEach((participant) => {
      byId.set(participant.id, participant)
    })

    return Array.from(byId.values())
  }, [project])
}