
import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import { Folder, Plus, Users } from 'lucide-react'
import { toast } from 'sonner'

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useProjects } from '@/context/ProjectsContext'
import {
  btnOutline,
  btnPrimary,
  fieldClass,
  labelClass,
  textareaClass,
} from '@/components/ui/task-ui'

// Ako korisnik ne unese kljuc, pravimo ga od naziva.
function generateProjectKey(name: string) {
  const words = name.trim().split(/\s+/).filter(Boolean)

  if (words.length === 0) return ''

  const raw =
    words.length === 1 ? words[0] : words.map((word) => word[0]).join('')

  return raw.replace(/[^a-zA-Z0-9]/g, '').slice(0, 4).toUpperCase()
}

export default function Projects() {
  const navigate = useNavigate()
  const { projects, isLoading, loadProjects, createProject } = useProjects()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [name, setName] = useState('')
  const [projectKey, setProjectKey] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    loadProjects().catch(() => {
      // Greska je vec prikazana iz ProjectsProvider-a.
    })
  }, [loadProjects])

  function openDialog() {
    setName('')
    setProjectKey('')
    setDescription('')
    setError('')
    setDialogOpen(true)
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    const trimmedName = name.trim()
    const key = (
      projectKey.trim() || generateProjectKey(trimmedName)
    ).toUpperCase()

    if (!trimmedName) {
      setError('Project name is required.')
      return
    }

    if (!/^[A-Z0-9]{2,10}$/.test(key)) {
      setError('Key must contain 2-10 letters or digits, e.g. MJ.')
      return
    }

    if (projects.some((project) => project.key.toUpperCase() === key)) {
      setError('This key is already used by another project.')
      return
    }

    setIsSaving(true)

    try {
      await createProject({
        name: trimmedName,
        key,
        description: description.trim(),
      })

      toast.success(`Project "${trimmedName}" created.`)
      setDialogOpen(false)
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const data = err.response?.data
        const firstFieldError = data?.errors
          ? (Object.values(data.errors)[0] as string | undefined)
          : undefined

        if (!err.response) {
          setError('Cannot connect to the server. Is the backend running?')
        } else {
          setError(
            firstFieldError ??
              data?.message ??
              'Could not create the project.',
          )
        }
      } else {
        setError('An unexpected error occurred.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Projects</h1>
          <p className="mt-0.5 text-xs text-[#6b7280]">
            Pick a project to open its board.
          </p>
        </div>

        <button type="button" onClick={openDialog} className={btnPrimary}>
          <Plus size={16} />
          New project
        </button>
      </div>

      {isLoading ? (
        <p className="py-16 text-center text-sm text-[#6b7280]">
          Loading projects...
        </p>
      ) : projects.length === 0 ? (
        <div className="rounded-xl border border-dashed border-[#d9dde4] bg-white px-6 py-16 text-center">
          <Folder className="mx-auto mb-3 text-[#9aa1ad]" size={28} />
          <h2 className="text-sm font-semibold">No projects yet</h2>
          <p className="mt-1 text-xs text-[#6b7280]">
            Create your first project to get started.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <button
              key={project.id}
              type="button"
              onClick={() => navigate(`/projects/${project.id}`)}
              className="rounded-xl border border-[#e6e8ec] bg-white p-4 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-[#cfd4dd] hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#9aa6ba]"
            >
              <div className="flex items-center gap-2">
                <span className="inline-flex h-6 w-6 items-center justify-center rounded-md bg-[#eceef2] text-[#4b5563]">
                  <Folder size={13} />
                </span>

                <span className="font-mono text-[11px] text-[#9aa1ad]">
                  {project.key}
                </span>
              </div>

              <h2 className="mt-3 text-sm font-semibold">
                {project.name}
              </h2>

              <p className="mt-1 line-clamp-2 min-h-8 text-xs leading-4 text-[#6b7280]">
                {project.description || 'No description'}
              </p>

              <div className="mt-4 flex items-center justify-between gap-3">
                <span className="text-xs text-[#6b7280]">
                  {project.taskCount}{' '}
                  {project.taskCount === 1 ? 'task' : 'tasks'}
                </span>

                <span className="flex items-center gap-1 text-xs text-[#6b7280]">
                  <Users size={14} />
                  {project.participants.length}{' '}
                  {project.participants.length === 1
                    ? 'member'
                    : 'members'}
                </span>
              </div>

              <div className="mt-3 border-t border-[#eef0f3] pt-3">
                <p className="text-xs text-[#6b7280]">
                  Creator:{' '}
                  <span className="font-medium text-[#374151]">
                    {project.owner?.name ?? 'Unknown'}
                  </span>
                </p>
              </div>
            </button>
          ))}
        </div>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="w-[calc(100%-2rem)] max-w-[480px] bg-white p-6 text-[#111827] shadow-2xl ring-1 ring-black/10 sm:max-w-[480px]">
          <DialogHeader>
            <DialogTitle className="text-lg font-semibold">
              Create project
            </DialogTitle>
          </DialogHeader>

          <form onSubmit={handleCreate} className="space-y-4">
            <div>
              <label htmlFor="project-name" className={labelClass}>
                Name
              </label>

              <input
                id="project-name"
                autoFocus
                value={name}
                onChange={(event) => {
                  setName(event.target.value)
                  setError('')
                }}
                maxLength={80}
                className={fieldClass}
              />
            </div>

            <div>
              <label htmlFor="project-key" className={labelClass}>
                Key (e.g. MJ)
              </label>

              <input
                id="project-key"
                value={projectKey}
                onChange={(event) => {
                  setProjectKey(
                    event.target.value
                      .replace(/[^a-zA-Z0-9]/g, '')
                      .toUpperCase()
                      .slice(0, 10),
                  )
                  setError('')
                }}
                placeholder={generateProjectKey(name)}
                className={fieldClass}
              />
            </div>

            <div>
              <label htmlFor="project-description" className={labelClass}>
                Description
              </label>

              <textarea
                id="project-description"
                rows={3}
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                maxLength={500}
                className={textareaClass}
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

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setDialogOpen(false)}
                disabled={isSaving}
                className={btnOutline}
              >
                Cancel
              </button>

              <button
                type="submit"
                disabled={isSaving}
                className={btnPrimary}
              >
                {isSaving ? 'Creating...' : 'Create'}
              </button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
