
import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'

import api from '@/api/axios'

export type TaskStatus = 'backlog' | 'todo' | 'in-progress' | 'done'
export type Priority = 'Low' | 'Medium' | 'High'
export type TaskType = 'Bug' | 'Task' | 'Story'

export interface Task {
  id: string
  key: string
  number: number
  title: string
  description: string
  status: TaskStatus
  priority: Priority
  type: TaskType
  assignee: string
  assigneeId: number | null
  createdAt: string
  projectId: string
}

export interface TaskComment {
  id: number
  text: string
  author: string
  createdAt: string
  hasImage: boolean
}

// Podaci koje korisnik unosi za kreiranje i izmenu taska.
export interface TaskInput {
  title: string
  description: string
  status: TaskStatus
  priority: Priority
  type: TaskType
  assigneeId: number | null
}

interface TasksContextType {
  tasks: Task[]
  loadTasks: (projectId: string) => Promise<void>
  addTask: (projectId: string, input: TaskInput) => Promise<Task>
  updateTask: (id: string, changes: Partial<TaskInput>) => Promise<Task>
  deleteTask: (id: string) => Promise<void>
  loadComments: (taskId: string) => Promise<TaskComment[]>
  addComment: (
    taskId: string,
    text: string,
    image?: File | null,
  ) => Promise<TaskComment>
  updateComment: (
    taskId: string,
    commentId: number,
    text: string,
  ) => Promise<TaskComment>
  deleteComment: (taskId: string, commentId: number) => Promise<void>
}

const TasksContext = createContext<TasksContextType | undefined>(undefined)

type BackendUser = {
  id: number
  name: string
  email: string
}

type BackendTask = {
  id: number
  key: string
  number: number
  title: string
  description: string | null
  status: string
  priority: string
  type: string
  projectId: number
  assignee: BackendUser | null
  createdAt: string
}

type BackendComment = {
  id: number
  text: string
  createdAt: string
  author: BackendUser | null
  hasImage: boolean
}

const statusFromBackend: Record<string, TaskStatus> = {
  BACKLOG: 'backlog',
  TODO: 'todo',
  IN_PROGRESS: 'in-progress',
  DONE: 'done',
}

const statusToBackend: Record<TaskStatus, string> = {
  backlog: 'BACKLOG',
  todo: 'TODO',
  'in-progress': 'IN_PROGRESS',
  done: 'DONE',
}

const priorityFromBackend: Record<string, Priority> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
}

const priorityToBackend: Record<Priority, string> = {
  Low: 'LOW',
  Medium: 'MEDIUM',
  High: 'HIGH',
}

const typeFromBackend: Record<string, TaskType> = {
  BUG: 'Bug',
  TASK: 'Task',
  STORY: 'Story',
}

const typeToBackend: Record<TaskType, string> = {
  Bug: 'BUG',
  Task: 'TASK',
  Story: 'STORY',
}

function normalizeTask(raw: BackendTask): Task {
  return {
    id: String(raw.id),
    key: raw.key,
    number: raw.number,
    title: raw.title,
    description: raw.description ?? '',
    status: statusFromBackend[raw.status] ?? 'backlog',
    priority: priorityFromBackend[raw.priority] ?? 'Medium',
    type: typeFromBackend[raw.type] ?? 'Task',
    assignee: raw.assignee?.name ?? '',
    assigneeId: raw.assignee?.id ?? null,
    createdAt: raw.createdAt,
    projectId: String(raw.projectId),
  }
}

// Pretvaranje komentara sa backenda u frontend model.
function normalizeComment(raw: BackendComment): TaskComment {
  return {
    id: raw.id,
    text: raw.text,
    author: raw.author?.name ?? 'Unknown user',
    createdAt: raw.createdAt,
    hasImage: raw.hasImage ?? false,
  }
}

function toBackendRequest(input: TaskInput) {
  return {
    title: input.title.trim(),
    description: input.description,
    type: typeToBackend[input.type],
    priority: priorityToBackend[input.priority],
    status: statusToBackend[input.status],
    assigneeId: input.assigneeId,
  }
}

export function TasksProvider({ children }: { children: ReactNode }) {
  const [tasks, setTasks] = useState<Task[]>([])
  const tasksRef = useRef<Task[]>([])

  const commit = useCallback((updater: (current: Task[]) => Task[]) => {
    tasksRef.current = updater(tasksRef.current)
    setTasks(tasksRef.current)
  }, [])

  const loadTasks = useCallback(
    async (projectId: string) => {
      const response = await api.get<BackendTask[]>(
        `/projects/${projectId}/tasks`,
      )

      const loaded = response.data.map(normalizeTask)

      commit((current) => [
        ...current.filter((task) => task.projectId !== projectId),
        ...loaded,
      ])
    },
    [commit],
  )

  const addTask = useCallback(
    async (projectId: string, input: TaskInput): Promise<Task> => {
      const response = await api.post<BackendTask>(
        `/projects/${projectId}/tasks`,
        toBackendRequest(input),
      )

      const created = normalizeTask(response.data)

      commit((current) => [...current, created])

      return created
    },
    [commit],
  )

  const updateTask = useCallback(
    async (id: string, changes: Partial<TaskInput>): Promise<Task> => {
      const existing = tasksRef.current.find((task) => task.id === id)

      if (!existing) {
        throw new Error('Task was not found in the current task list.')
      }

      const onlyStatusChanged =
        changes.status !== undefined && Object.keys(changes).length === 1

      if (onlyStatusChanged) {
        const newStatus = changes.status as TaskStatus

        commit((current) =>
          current.map((task) =>
            task.id === id ? { ...task, status: newStatus } : task,
          ),
        )

        try {
          const response = await api.patch<BackendTask>(
            `/tasks/${id}/status`,
            { status: statusToBackend[newStatus] },
          )

          const updated = normalizeTask(response.data)

          commit((current) =>
            current.map((task) => (task.id === id ? updated : task)),
          )

          return updated
        } catch (error) {
          commit((current) =>
            current.map((task) => (task.id === id ? existing : task)),
          )

          throw error
        }
      }

      const merged: TaskInput = {
        title: changes.title ?? existing.title,
        description: changes.description ?? existing.description,
        status: changes.status ?? existing.status,
        priority: changes.priority ?? existing.priority,
        type: changes.type ?? existing.type,
        assigneeId:
          changes.assigneeId !== undefined
            ? changes.assigneeId
            : existing.assigneeId,
      }

      const response = await api.put<BackendTask>(
        `/tasks/${id}`,
        toBackendRequest(merged),
      )

      const updated = normalizeTask(response.data)

      commit((current) =>
        current.map((task) => (task.id === id ? updated : task)),
      )

      return updated
    },
    [commit],
  )

  const deleteTask = useCallback(
    async (id: string): Promise<void> => {
      await api.delete(`/tasks/${id}`)

      commit((current) => current.filter((task) => task.id !== id))
    },
    [commit],
  )

  // Ucitavanje komentara taska.
  const loadComments = useCallback(
    async (taskId: string): Promise<TaskComment[]> => {
      const response = await api.get<BackendComment[]>(
        `/tasks/${taskId}/comments`,
      )

      return response.data.map(normalizeComment)
    },
    [],
  )

  // Dodavanje komentara sa opcionom slikom.
  const addComment = useCallback(
  async (
    taskId: string,
    text: string,
    image?: File | null,
  ): Promise<TaskComment> => {
    const formData = new FormData()

    formData.append('text', text.trim())

    if (image) {
      formData.append('image', image)
    }

    const response = await api.post<BackendComment>(
      `/tasks/${taskId}/comments`,
      formData,
      {
        headers: {
          'Content-Type': undefined,
        },
      },
    )

    return normalizeComment(response.data)
  },
  [],
)

  // Izmena teksta komentara.
  const updateComment = useCallback(
    async (
      taskId: string,
      commentId: number,
      text: string,
    ): Promise<TaskComment> => {
      const response = await api.patch<BackendComment>(
        `/tasks/${taskId}/comments/${commentId}`,
        { text },
      )

      return normalizeComment(response.data)
    },
    [],
  )

  // Brisanje komentara.
  const deleteComment = useCallback(
    async (taskId: string, commentId: number): Promise<void> => {
      await api.delete(`/tasks/${taskId}/comments/${commentId}`)
    },
    [],
  )

  const value = useMemo(
    () => ({
      tasks,
      loadTasks,
      addTask,
      updateTask,
      deleteTask,
      loadComments,
      addComment,
      updateComment,
      deleteComment,
    }),
    [
      tasks,
      loadTasks,
      addTask,
      updateTask,
      deleteTask,
      loadComments,
      addComment,
      updateComment,
      deleteComment,
    ],
  )

  return (
    <TasksContext.Provider value={value}>
      {children}
    </TasksContext.Provider>
  )
}

export function useTasks() {
  const context = useContext(TasksContext)

  if (!context) {
    throw new Error('useTasks must be used within a TasksProvider')
  }

  return context
}
