
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { toast } from 'sonner'

import api from '@/api/axios'

export interface ProjectMember {
  id: number
  name: string
  email: string
}

export interface Project {
  id: string
  name: string
  key: string
  description: string
  taskCount: number
  owner: ProjectMember | null
  participants: ProjectMember[]
}

interface BackendProject {
  id: number
  name: string
  key: string
  description: string | null
  taskCount: number
  owner: ProjectMember | null
  participants: ProjectMember[] | null
}

interface ProjectInput {
  name: string
  key: string
  description: string
}

interface ProjectsContextType {
  projects: Project[]
  isLoading: boolean
  loadProjects: () => Promise<void>
  createProject: (input: ProjectInput) => Promise<Project>
  addParticipant: (
    projectId: string,
    userId: number,
  ) => Promise<Project>
}

const ProjectsContext = createContext<ProjectsContextType | undefined>(
  undefined,
)

function normalizeProject(raw: BackendProject): Project {
  return {
    id: String(raw.id),
    name: raw.name,
    key: raw.key,
    description: raw.description ?? '',
    taskCount: raw.taskCount ?? 0,
    owner: raw.owner ?? null,
    participants: raw.participants ?? [],
  }
}

export function ProjectsProvider({ children }: { children: ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([])
  const [isLoading, setIsLoading] = useState(true)

  const loadProjects = useCallback(async () => {
    try {
      const response = await api.get<BackendProject[]>('/projects')
      setProjects(response.data.map(normalizeProject))
    } finally {
      setIsLoading(false)
    }
  }, [])

  const createProject = useCallback(
    async (input: ProjectInput): Promise<Project> => {
      const response = await api.post<BackendProject>(
        '/projects',
        input,
      )

      const created = normalizeProject(response.data)

      setProjects((current) => [...current, created])

      return created
    },
    [],
  )

  const addParticipant = useCallback(
    async (
      projectId: string,
      userId: number,
    ): Promise<Project> => {
      const response = await api.post<BackendProject>(
        `/projects/${projectId}/participants/${userId}`,
      )

      const updated = normalizeProject(response.data)

      setProjects((current) =>
        current.map((project) =>
          project.id === projectId ? updated : project,
        ),
      )

      return updated
    },
    [],
  )

  useEffect(() => {
    loadProjects().catch(() => {
      toast.error('Could not load projects from the server.')
    })
  }, [loadProjects])

  const value = useMemo(
    () => ({
      projects,
      isLoading,
      loadProjects,
      createProject,
      addParticipant,
    }),
    [projects, isLoading, loadProjects, createProject, addParticipant],
  )

  return (
    <ProjectsContext.Provider value={value}>
      {children}
    </ProjectsContext.Provider>
  )
}

export function useProjects() {
  const context = useContext(ProjectsContext)

  if (!context) {
    throw new Error('useProjects must be used within a ProjectsProvider')
  }

  return context
}
