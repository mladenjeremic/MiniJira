import { useEffect, useState, type FormEvent } from 'react'
import { toast } from 'sonner'


import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  useTasks,
  type Priority,
  type TaskStatus,
  type TaskType,
} from '@/context/TasksContext'
import type { AppUser } from '@/hooks/useUsers'
import { getStoredUser } from '@/lib/session'
import { btnOutline, btnPrimary, columns, fieldClass, labelClass, priorities, taskTypes, textareaClass } from './ui/task-ui'
import SelectField from './ui/SelectField'

interface CreateTaskDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  projectId: string
  users: AppUser[]
  defaultStatus?: TaskStatus
}

export default function CreateTaskDialog({
  open,
  onOpenChange,
  projectId,
  users,
  defaultStatus = 'todo',
}: CreateTaskDialogProps) {
  const { addTask } = useTasks()

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [type, setType] = useState<TaskType>('Task')
  const [priority, setPriority] = useState<Priority>('Medium')
  const [status, setStatus] = useState<TaskStatus>(defaultStatus)
  const [assigneeId, setAssigneeId] = useState('')
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  // Svaki put kad se dijalog otvori, forma krece iznova.
  // Podrazumevani assignee je trenutno ulogovan korisnik.
  useEffect(() => {
    if (!open) return

    const me = getStoredUser()

    setTitle('')
    setDescription('')
    setType('Task')
    setPriority('Medium')
    setStatus(defaultStatus)
    setAssigneeId(me?.id ? String(me.id) : '')
    setError('')
  }, [open, defaultStatus])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const trimmedTitle = title.trim()

    if (!trimmedTitle) {
      setError('Title is required.')
      return
    }

    setIsSaving(true)

    try {
      const created = await addTask(projectId, {
        title: trimmedTitle,
        description: description.trim(),
        type,
        priority,
        status,
        assigneeId: assigneeId ? Number(assigneeId) : null,
      })

      toast.success(`Task ${created.key} created.`)
      onOpenChange(false)
    } catch {
      setError('Could not create the task. Please try again.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[calc(100%-2rem)] max-w-[520px] bg-white p-6 text-[#111827] shadow-2xl ring-1 ring-black/10 sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle className="text-lg font-semibold">Create task</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="task-title" className={labelClass}>
              Title
            </label>

            <input
              id="task-title"
              autoFocus
              value={title}
              onChange={(event) => {
                setTitle(event.target.value)
                setError('')
              }}
              placeholder="Short summary"
              maxLength={200}
              className={fieldClass}
            />
          </div>

          <div>
            <label htmlFor="task-description" className={labelClass}>
              Description
            </label>

            <textarea
              id="task-description"
              rows={3}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Add detail..."
              className={textareaClass}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="task-type" className={labelClass}>
                Type
              </label>

              <SelectField
                id="task-type"
                value={type}
                onChange={(value) => setType(value as TaskType)}
                options={taskTypes.map((item) => ({ value: item, label: item }))}
              />
            </div>

            <div>
              <label htmlFor="task-priority" className={labelClass}>
                Priority
              </label>

              <SelectField
                id="task-priority"
                value={priority}
                onChange={(value) => setPriority(value as Priority)}
                options={priorities.map((item) => ({ value: item, label: item }))}
              />
            </div>

            <div>
              <label htmlFor="task-status" className={labelClass}>
                Status
              </label>

              <SelectField
                id="task-status"
                value={status}
                onChange={(value) => setStatus(value as TaskStatus)}
                options={columns.map((column) => ({
                  value: column.id,
                  label: column.title,
                }))}
              />
            </div>

            <div>
              <label htmlFor="task-assignee" className={labelClass}>
                Assignee
              </label>

              <SelectField
                id="task-assignee"
                value={assigneeId}
                onChange={setAssigneeId}
                options={[
                  { value: '', label: 'Unassigned' },
                  ...users.map((user) => ({
                    value: String(user.id),
                    label: user.name,
                  })),
                ]}
              />
            </div>
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
              onClick={() => onOpenChange(false)}
              disabled={isSaving}
              className={btnOutline}
            >
              Cancel
            </button>

            <button type="submit" disabled={isSaving} className={btnPrimary}>
              {isSaving ? 'Creating...' : 'Create'}
            </button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
