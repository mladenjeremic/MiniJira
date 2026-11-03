
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Send, Trash2, Edit2, Check, X, ImagePlus } from 'lucide-react'
import { toast } from 'sonner'
import api from '@/api/axios'

import {
  Dialog,
  DialogContent,
  DialogTitle,
} from '@/components/ui/dialog'

import {
  useTasks,
  type Priority,
  type Task,
  type TaskComment,
  type TaskInput,
  type TaskStatus,
  type TaskType,
} from '@/context/TasksContext'

import type { AppUser } from '@/hooks/useUsers'
import { formatDate, formatDateTime } from '@/lib/format'

import {
  btnDanger,
  btnOutline,
  columns,
  fieldClass,
  KeyBadge,
  labelClass,
  priorities,
  PriorityBadge,
  taskTypes,
  textareaClass,
  TypeIcon,
  UserAvatar,
} from './task-ui'

import SelectField from './SelectField'

interface TaskDetailDialogProps {
  task: Task | null
  users: AppUser[]
  onClose: () => void
}

export default function TaskDetailDialog({
  task,
  users,
  onClose,
}: TaskDetailDialogProps) {
  return (
    <Dialog
      open={task !== null}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      <DialogContent className="max-h-[90vh] w-[calc(100%-2rem)] max-w-[820px] overflow-y-auto bg-white p-6 text-[#111827] shadow-2xl ring-1 ring-black/10 sm:max-w-[820px]">
        {task && (
          <DetailBody
            key={task.id}
            task={task}
            users={users}
            onClose={onClose}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function DetailBody({
  task,
  users,
  onClose,
}: {
  task: Task
  users: AppUser[]
  onClose: () => void
}) {
  const {
    updateTask,
    deleteTask,
    loadComments,
    addComment,
    updateComment,
    deleteComment,
  } = useTasks()

  const [title, setTitle] = useState(task.title)
  const [description, setDescription] = useState(task.description)

  const [comments, setComments] = useState<TaskComment[]>([])
  const [commentsLoading, setCommentsLoading] = useState(true)

  const [commentText, setCommentText] = useState('')
  const [selectedImage, setSelectedImage] = useState<File | null>(null)
  const [isSending, setIsSending] = useState(false)

  // Stanja za inline izmenu komentara
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null)
  const [editingText, setEditingText] = useState('')

  const [confirmDelete, setConfirmDelete] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  // Ucitavanje komentara
  useEffect(() => {
    let active = true

    loadComments(task.id)
      .then((items) => {
        if (active) setComments(items)
      })
      .catch(() => toast.error('Could not load comments.'))
      .finally(() => {
        if (active) setCommentsLoading(false)
      })

    return () => {
      active = false
    }
  }, [task.id, loadComments])

  // Cuvanje izmena taska
  async function save(changes: Partial<TaskInput>) {
    try {
      await updateTask(task.id, changes)
      toast.success('Task updated.')
    } catch {
      toast.error('Could not save the change.')
      setTitle(task.title)
      setDescription(task.description)
    }
  }

  function handleTitleBlur() {
    const next = title.trim()

    if (!next) {
      setTitle(task.title)
      toast.error('Title cannot be empty.')
      return
    }

    if (next !== task.title) {
      void save({ title: next })
    }
  }

  function handleDescriptionBlur() {
    if (description !== task.description) {
      void save({ description })
    }
  }

  // Izbor slike
  function handleImageSelect(
    event: ChangeEvent<HTMLInputElement>
  ) {
    const file = event.target.files?.[0]

    if (!file) return

    const allowedTypes = [
      'image/png',
      'image/jpeg',
      'image/gif',
      'image/webp',
    ]

    if (!allowedTypes.includes(file.type)) {
      toast.error('Only PNG, JPG, GIF and WEBP images are allowed.')
      event.target.value = ''
      return
    }

    if (file.size > 5 * 1024 * 1024) {
      toast.error('Image must be smaller than 5 MB.')
      event.target.value = ''
      return
    }

    setSelectedImage(file)
  }

  // Dodavanje komentara sa opcionom slikom
  async function handleAddComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const text = commentText.trim()

    if (!text && !selectedImage) return

    setIsSending(true)

    try {
      const created = await addComment(
        task.id,
        text,
        selectedImage
      )

      setComments((current) => [...current, created])

      setCommentText('')
      setSelectedImage(null)

      toast.success('Comment added.')
    } catch {
      toast.error('Could not add the comment.')
    } finally {
      setIsSending(false)
    }
  }

  // Pokretanje izmene komentara
  function startEditing(comment: TaskComment) {
    setEditingCommentId(comment.id)
    setEditingText(comment.text)
  }

  // Otkazivanje izmene
  function cancelEditing() {
    setEditingCommentId(null)
    setEditingText('')
  }

  // Cuvanje izmenjenog komentara
  async function handleUpdateComment(commentId: number) {
    const text = editingText.trim()

    if (!text) return

    try {
      const updated = await updateComment(
        task.id,
        commentId,
        text
      )

      setComments((current) =>
        current.map((c) =>
          c.id === commentId ? updated : c
        )
      )

      setEditingCommentId(null)
      setEditingText('')

      toast.success('Comment updated.')
    } catch {
      toast.error('Could not update the comment.')
    }
  }

  // Brisanje komentara
  async function handleDeleteComment(commentId: number) {
    try {
      await deleteComment(task.id, commentId)

      setComments((current) =>
        current.filter((c) => c.id !== commentId)
      )

      toast.success('Comment deleted.')
    } catch {
      toast.error('Could not delete the comment.')
    }
  }

  // Brisanje taska
  async function handleDelete() {
    setIsDeleting(true)

    try {
      await deleteTask(task.id)

      toast.success(`Task ${task.key} deleted.`)
      onClose()
    } catch {
      toast.error('Could not delete the task.')
      setIsDeleting(false)
    }
  }

  return (
    <>
      <DialogTitle className="sr-only">
        {task.title}
      </DialogTitle>

      {/* Zaglavlje taska */}
      <div className="pr-8">
        <div className="flex items-center gap-2">
          <TypeIcon type={task.type} />
          <KeyBadge>{task.key}</KeyBadge>
          <PriorityBadge priority={task.priority} />
        </div>

        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          onBlur={handleTitleBlur}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.currentTarget.blur()
            }
          }}
          aria-label="Task title"
          maxLength={200}
          className="-mx-2 mt-3 w-[calc(100%+1rem)] rounded-md border border-transparent bg-transparent px-2 py-1 text-base font-semibold text-[#111827] outline-none hover:border-[#e2e5ea] focus:border-[#9aa6ba] focus:bg-white"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-[1fr_220px]">

        {/* Leva kolona */}
        <div className="min-w-0 space-y-6">

          {/* Opis */}
          <div>
            <label
              htmlFor="detail-description"
              className={labelClass}
            >
              Description
            </label>

            <textarea
              id="detail-description"
              rows={5}
              value={description}
              onChange={(event) =>
                setDescription(event.target.value)
              }
              onBlur={handleDescriptionBlur}
              placeholder="Add detail..."
              className={textareaClass}
            />
          </div>

          {/* Komentari */}
          <section>
            <h3 className="text-sm font-semibold text-[#111827]">
              Comments
            </h3>

            {commentsLoading ? (
              <p className="mt-1 text-xs text-[#6b7280]">
                Loading comments...
              </p>
            ) : comments.length === 0 ? (
              <p className="mt-1 text-xs text-[#6b7280]">
                No comments yet.
              </p>
            ) : (
              <ul className="mt-3 space-y-4">
                {comments.map((comment) => (
                  <li
                    key={comment.id}
                    className="group flex items-start justify-between gap-3"
                  >
                    <div className="flex min-w-0 flex-1 gap-3">

                      <UserAvatar name={comment.author} />

                      <div className="min-w-0 flex-1">
                        <p className="text-xs">
                          <span className="font-semibold text-[#111827]">
                            {comment.author}
                          </span>{' '}

                          <span className="text-[#6b7280]">
                            · {formatDateTime(comment.createdAt)}
                          </span>
                        </p>

                        {editingCommentId === comment.id ? (
                          <div className="mt-1 flex items-end gap-2">
                            <textarea
                              value={editingText}
                              onChange={(event) =>
                                setEditingText(event.target.value)
                              }
                              className="w-full rounded-md border bg-white p-1.5 text-sm text-[#111827] focus:outline-none focus:ring-1 focus:ring-[#9aa6ba]"
                              rows={2}
                              maxLength={2000}
                              aria-label="Edit comment"
                            />

                            <div className="flex flex-col gap-1">
                              <button
                                type="button"
                                onClick={() =>
                                  void handleUpdateComment(comment.id)
                                }
                                disabled={!editingText.trim()}
                                className="rounded-md p-1 text-green-600 transition hover:bg-green-50 disabled:opacity-40"
                                title="Save change"
                                aria-label="Save comment"
                              >
                                <Check size={16} />
                              </button>

                              <button
                                type="button"
                                onClick={cancelEditing}
                                className="rounded-md p-1 text-gray-500 transition hover:bg-gray-100"
                                title="Cancel"
                                aria-label="Cancel editing"
                              >
                                <X size={16} />
                              </button>
                            </div>
                          </div>
                        ) : (
                          <p className="mt-0.5 whitespace-pre-wrap break-words text-sm text-[#374151]">
                            {comment.text}
                          </p>
                        )}

                        {/* Prikaz slike komentara */}
                        {comment.hasImage && (
                          <CommentImage
                            taskId={task.id}
                            commentId={comment.id}
                          />
                        )}
                      </div>
                    </div>

                    {/* Dugmad za izmenu i brisanje */}
                    {editingCommentId !== comment.id && (
                      <div className="flex shrink-0 gap-1 opacity-100 transition sm:opacity-0 sm:group-hover:opacity-100">
                        <button
                          type="button"
                          onClick={() => startEditing(comment)}
                          className="rounded-md p-1 text-gray-500 transition hover:bg-gray-100 hover:text-blue-600"
                          title="Edit comment"
                          aria-label="Edit comment"
                        >
                          <Edit2 size={16} />
                        </button>

                        <button
                          type="button"
                          onClick={() =>
                            void handleDeleteComment(comment.id)
                          }
                          className="rounded-md p-1 text-gray-500 transition hover:bg-gray-100 hover:text-red-600"
                          title="Delete comment"
                          aria-label="Delete comment"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}

            {/* Forma za novi komentar */}
            <form
              onSubmit={handleAddComment}
              className="mt-4 space-y-2"
            >
              <div className="flex gap-2">
                <input
                  value={commentText}
                  onChange={(event) =>
                    setCommentText(event.target.value)
                  }
                  placeholder="Add a comment..."
                  aria-label="Add a comment"
                  maxLength={2000}
                  className={fieldClass}
                />

                {/* Izbor slike */}
                <label
                  className="inline-flex h-10 w-10 shrink-0 cursor-pointer items-center justify-center rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-100"
                  title="Attach image"
                >
                  <ImagePlus size={18} />

                  <input
                    type="file"
                    accept="image/png,image/jpeg,image/gif,image/webp"
                    className="hidden"
                    onChange={handleImageSelect}
                  />
                </label>

                {/* Slanje komentara */}
                <button
                  type="submit"
                  disabled={
                    isSending ||
                    (!commentText.trim() && !selectedImage)
                  }
                  aria-label="Send comment"
                  className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#151c2e] text-white shadow-sm transition hover:bg-[#262f47] disabled:bg-[#9aa1ad]"
                >
                  <Send size={16} />
                </button>
              </div>

              {/* Izabrana slika */}
              {selectedImage && (
                <div className="flex items-center gap-2 rounded-lg border border-gray-200 p-2">
                  <ImagePlus size={18} className="shrink-0 text-gray-500" />

                  <span className="min-w-0 flex-1 truncate text-sm text-gray-600">
                    {selectedImage.name}
                  </span>

                  <button
                    type="button"
                    onClick={() => setSelectedImage(null)}
                    className="shrink-0 text-sm text-red-500 hover:text-red-700"
                  >
                    Remove
                  </button>
                </div>
              )}
            </form>
          </section>
        </div>

        {/* Desna kolona */}
        <aside className="space-y-4">

          <div>
            <label
              htmlFor="detail-status"
              className={labelClass}
            >
              Status
            </label>

            <SelectField
              id="detail-status"
              value={task.status}
              onChange={(value) =>
                void save({ status: value as TaskStatus })
              }
              options={columns.map((column) => ({
                value: column.id,
                label: column.title,
              }))}
            />
          </div>

          <div>
            <label
              htmlFor="detail-type"
              className={labelClass}
            >
              Type
            </label>

            <SelectField
              id="detail-type"
              value={task.type}
              onChange={(value) =>
                void save({ type: value as TaskType })
              }
              options={taskTypes.map((type) => ({
                value: type,
                label: type,
              }))}
            />
          </div>

          <div>
            <label
              htmlFor="detail-priority"
              className={labelClass}
            >
              Priority
            </label>

            <SelectField
              id="detail-priority"
              value={task.priority}
              onChange={(value) =>
                void save({ priority: value as Priority })
              }
              options={priorities.map((priority) => ({
                value: priority,
                label: priority,
              }))}
            />
          </div>

          <div>
            <label
              htmlFor="detail-assignee"
              className={labelClass}
            >
              Assignee
            </label>

            <SelectField
              id="detail-assignee"
              value={
                task.assigneeId === null
                  ? ''
                  : String(task.assigneeId)
              }
              onChange={(value) =>
                void save({
                  assigneeId: value ? Number(value) : null,
                })
              }
              options={[
                { value: '', label: 'Unassigned' },
                ...users.map((user) => ({
                  value: String(user.id),
                  label: user.name,
                })),
              ]}
            />
          </div>

          <p className="text-xs text-[#6b7280]">
            Created {formatDate(task.createdAt)}
          </p>

          {/* Brisanje taska */}
          {confirmDelete ? (
            <div className="space-y-2">
              <p className="text-sm text-[#374151]">
                Delete this task and all its comments?
              </p>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setConfirmDelete(false)}
                  disabled={isDeleting}
                  className={`${btnOutline} flex-1 px-2`}
                >
                  Cancel
                </button>

                <button
                  type="button"
                  onClick={() => void handleDelete()}
                  disabled={isDeleting}
                  className={`${btnDanger} flex-1 px-2`}
                >
                  {isDeleting ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setConfirmDelete(true)}
              className={`${btnDanger} h-10 w-full`}
            >
              Delete task
            </button>
          )}
        </aside>
      </div>
    </>
  )
}

// Posebna komponenta za ucitavanje slike uz autentifikaciju
function CommentImage({
  taskId,
  commentId,
}: {
  taskId: string
  commentId: number
}) {
  const [imageUrl, setImageUrl] = useState<string | null>(null)
  const [imageError, setImageError] = useState(false)

  useEffect(() => {
    let active = true
    let objectUrl: string | null = null

    async function loadImage() {
      try {
        const response = await api.get(
          `/tasks/${taskId}/comments/${commentId}/image`,
          {
            responseType: 'blob',
          }
        )

        const url = URL.createObjectURL(response.data)
        objectUrl = url

        if (active) {
          setImageUrl(url)
        } else {
          URL.revokeObjectURL(url)
        }
      } catch {
        if (active) {
          setImageError(true)
        }
      }
    }

    void loadImage()

    return () => {
      active = false

      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [taskId, commentId])

  if (imageError) {
    return (
      <p className="mt-2 text-xs text-red-500">
        Could not load image.
      </p>
    )
  }

  if (!imageUrl) {
    return (
      <p className="mt-2 text-xs text-gray-400">
        Loading image...
      </p>
    )
  }

  return (
    <img
      src={imageUrl}
      alt="Comment attachment"
      className="mt-2 max-h-64 max-w-full rounded-lg border border-gray-200 object-contain"
    />
  )
}
