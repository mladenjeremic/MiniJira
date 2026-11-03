import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  DndContext,
  DragOverlay,
  MouseSensor,
  TouchSensor,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core'
import {
  CircleDashed,
  Circle,
  Clock3,
  CheckCircle2,
  UserPlus,
} from 'lucide-react'
import { toast } from 'sonner'

import CreateTaskDialog from '@/components/CreateTaskDialog'
import { useProjects } from '@/context/ProjectsContext'
import { useTasks, type Task } from '@/context/TasksContext'
import { useUsers } from '@/hooks/useUsers'
import { useProjectMembers } from '@/hooks/useProjectMembers' // NOVO
import { getStoredUser } from '@/lib/session' // NOVO
import {
  columns,
  KeyBadge,
  PriorityBadge,
  TypeIcon,
  UserAvatar,
} from '@/components/ui/task-ui'
import FilterBar, {
  applyFilters,
  emptyFilters,
  type Filters,
} from '@/components/ui/FilterBar'
import ProjectHeader from '@/components/ui/ProjectHeader'
import TaskDetailDialog from '@/components/ui/TaskDetailDialog'
import NoProjectAccess from '@/components/ui/NoProjectAccess'

function TaskStatusIcon({ status }: { status: Task['status'] }) {
  switch (status) {
    case 'backlog':
      return <CircleDashed size={16} className="text-[#64748b]" />

    case 'todo':
      return <Circle size={16} className="text-[#64748b]" />

    case 'in-progress':
      return <Clock3 size={16} className="text-[#2563eb]" />

    case 'done':
      return <CheckCircle2 size={16} className="text-[#16a34a]" />

    default:
      return null
  }
}

/* Izgled jedne kartice zadatka */
function TaskCardView({
  task,
  className = '',
}: {
  task: Task
  className?: string
}) {
  return (
    <div
      className={`rounded-lg border border-[#e6e8ec] bg-white p-3 shadow-sm ${className}`}
    >
      <div className="flex items-start gap-2">
        <span className="mt-0.5">
          <TypeIcon type={task.type} size={15} />
        </span>

        <p className="break-words text-[13px] font-semibold leading-5 text-[#111827]">
          {task.title}
        </p>
      </div>

      <div className="mt-2.5 flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5">
          <KeyBadge>{task.key}</KeyBadge>
          <PriorityBadge priority={task.priority} />
        </div>

        <div className="flex items-center gap-2">
          <TaskStatusIcon status={task.status} />
          <UserAvatar name={task.assignee} size="sm" />
        </div>
      </div>
    </div>
  )
}

function DraggableCard({
  task,
  onOpen,
}: {
  task: Task
  onOpen: (taskId: string) => void
}) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: task.id,
  })

  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      onClick={() => {
        if (!isDragging) onOpen(task.id)
      }}
      className={`cursor-grab outline-none transition-opacity active:cursor-grabbing ${
        isDragging ? 'opacity-40' : 'opacity-100'
      }`}
    >
      <TaskCardView
        task={task}
        className="hover:border-[#cfd4dd] hover:shadow-md"
      />
    </div>
  )
}

function BoardColumn({
  id,
  title,
  tasks,
  onOpen,
}: {
  id: string
  title: string
  tasks: Task[]
  onOpen: (taskId: string) => void
}) {
  const { setNodeRef, isOver } = useDroppable({ id })

  return (
    <section className="flex min-w-[250px] flex-1 flex-col">
      <div className="mb-2 flex items-center justify-between px-1">
        <h2 className="text-[11px] font-semibold uppercase tracking-wider text-[#6b7280]">
          {title}
        </h2>

        <span className="text-[11px] font-medium text-[#9aa1ad]">
          {tasks.length}
        </span>
      </div>

      <div
        ref={setNodeRef}
        className={`min-h-[320px] flex-1 space-y-2.5 rounded-xl p-2.5 transition-colors ${
          isOver ? 'bg-[#dfe2e8] ring-2 ring-[#c4c9d4]' : 'bg-[#f3f4f6]'
        }`}
      >
        {tasks.map((task) => (
          <DraggableCard
            key={task.id}
            task={task}
            onOpen={onOpen}
          />
        ))}

        {tasks.length === 0 && (
          <p className="py-8 text-center text-xs text-[#9aa1ad]">
            No tasks
          </p>
        )}
      </div>
    </section>
  )
}

export default function Board() {
  const { id = '' } = useParams()

  const { projects, isLoading, addParticipant } = useProjects() // IZMENJENO: dodat isLoading
  const { tasks, loadTasks, updateTask } = useTasks()
  const users = useUsers()

  const project = projects.find((item) => item.id === id)

  // NOVO: članovi projekta (za assignee dropdown i filter) i provera da li sam vlasnik
  const members = useProjectMembers(project)
  const currentUser = getStoredUser()
  const isOwner = Boolean(
    project?.owner &&
      currentUser?.id !== undefined &&
      project.owner.id === currentUser.id,
  )

  const [filters, setFilters] = useState<Filters>(emptyFilters)
  const [createOpen, setCreateOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [activeId, setActiveId] = useState<string | null>(null)

  // State za dodavanje učesnika
  const [addMemberOpen, setAddMemberOpen] = useState(false)
  const [selectedUserId, setSelectedUserId] = useState('')
  const [isAddingMember, setIsAddingMember] = useState(false)

  // IZMENJENO: taskove učitavamo tek kad znamo da projekat postoji i da imamo pristup
  const hasProject = Boolean(project)

  useEffect(() => {
    if (!hasProject) return

    loadTasks(id).catch(() => toast.error('Failed to load project tasks.'))
  }, [id, hasProject, loadTasks])

  // Korisnici koji još nisu vlasnik niti učesnici ovog projekta
  const availableUsers = useMemo(
    () =>
      users.filter(
        (user) =>
          !project?.participants.some(
            (participant) =>
              String(participant.id) === String(user.id),
          ) &&
          String(project?.owner?.id) !== String(user.id),
      ),
    [users, project],
  )

  const projectTasks = useMemo(
    () => tasks.filter((task) => task.projectId === id),
    [tasks, id],
  )

  const visibleTasks = useMemo(
    () => applyFilters(projectTasks, filters),
    [projectTasks, filters],
  )

  const activeTask = activeId
    ? (projectTasks.find((task) => task.id === activeId) ?? null)
    : null

  const selectedTask = selectedId
    ? (projectTasks.find((task) => task.id === selectedId) ?? null)
    : null

  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 6 } }),
    useSensor(TouchSensor, {
      activationConstraint: { delay: 200, tolerance: 8 },
    }),
  )

  function handleDragStart(event: DragStartEvent) {
    setActiveId(String(event.active.id))
  }

  function handleDragEnd(event: DragEndEvent) {
    setActiveId(null)

    const { active, over } = event

    if (!over) return

    const task = projectTasks.find((item) => item.id === String(active.id))
    const target = columns.find((column) => column.id === over.id)

    if (!task || !target || task.status === target.id) return

    updateTask(task.id, { status: target.id })
      .then(() => toast.success(`${task.key} moved to ${target.title}.`))
      .catch(() => toast.error('Could not move the task. Please try again.'))
  }

  async function handleAddParticipant() {
    if (!selectedUserId || isAddingMember) return

    setIsAddingMember(true)

    try {
      await addParticipant(id, Number(selectedUserId))

      toast.success('Participant added successfully.')
      setAddMemberOpen(false)
      setSelectedUserId('')
    } catch {
      toast.error('Could not add participant. Please try again.')
    } finally {
      setIsAddingMember(false)
    }
  }

  // NOVO: projekat ne postoji ili korisnik nije vlasnik/učesnik
  if (!isLoading && !project) {
    return <NoProjectAccess />
  }

  // NOVO: učesnici bez vlasnika (vlasnik je već prikazan kao Owner)
  const otherParticipants =
    project?.participants.filter(
      (participant) => participant.id !== project.owner?.id,
    ) ?? []

  return (
    <div className="space-y-5">
      <ProjectHeader
        project={project}
        projectId={id}
        view="board"
        onNewTask={() => setCreateOpen(true)}
      />

      {/* Članovi projekta */}
      {project && (
        <section className="rounded-xl border border-[#e6e8ec] bg-white p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-[#111827]">
                Members
              </h2>

              <p className="mt-1 text-xs text-[#6b7280]">
                Project owner and participants
              </p>
            </div>

            {/* IZMENJENO: dugme vidi samo vlasnik projekta */}
            {isOwner && (
              <button
                type="button"
                onClick={() => {
                  setSelectedUserId('')
                  setAddMemberOpen(true)
                }}
                className="inline-flex items-center gap-2 rounded-lg bg-[#2563eb] px-3 py-2 text-sm font-medium text-white hover:bg-[#1d4ed8]"
              >
                <UserPlus size={16} />
                Add participant
              </button>
            )}
          </div>

          <div className="mt-4 flex flex-wrap gap-3">
            {project.owner && (
              <div
                key={`owner-${project.owner.id}`}
                className="flex items-center gap-3 rounded-lg border border-[#e6e8ec] px-3 py-2"
              >
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#dbeafe] text-sm font-semibold text-[#1d4ed8]">
                  {project.owner.name.charAt(0).toUpperCase()}
                </div>

                <div>
                  <p className="text-sm font-medium text-[#111827]">
                    {project.owner.name}
                  </p>
                  <p className="text-xs text-[#6b7280]">Owner</p>
                </div>
              </div>
            )}

            {otherParticipants.map((participant) => (
              <div
                key={participant.id}
                className="flex items-center gap-3 rounded-lg border border-[#e6e8ec] px-3 py-2"
              >
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#f3e8ff] text-sm font-semibold text-[#7e22ce]">
                  {participant.name.charAt(0).toUpperCase()}
                </div>

                <div>
                  <p className="text-sm font-medium text-[#111827]">
                    {participant.name}
                  </p>
                  <p className="text-xs text-[#6b7280]">Participant</p>
                </div>
              </div>
            ))}

            {otherParticipants.length === 0 && (
              <p className="text-sm text-[#6b7280]">
                No participants yet.
              </p>
            )}
          </div>
        </section>
      )}

      {/* Prozor za izbor učesnika */}
      {addMemberOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          onClick={() => {
            if (!isAddingMember) setAddMemberOpen(false)
          }}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-member-title"
            className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <h2
              id="add-member-title"
              className="text-lg font-semibold text-[#111827]"
            >
              Add participant
            </h2>

            <p className="mt-1 text-sm text-[#6b7280]">
              Choose a user to add to this project.
            </p>

            <label
              htmlFor="participant-select"
              className="mt-4 block text-sm font-medium text-[#374151]"
            >
              User
            </label>

            <select
              id="participant-select"
              value={selectedUserId}
              onChange={(event) => setSelectedUserId(event.target.value)}
              className="mt-2 w-full rounded-lg border border-[#d1d5db] bg-white px-3 py-2 text-sm outline-none focus:border-[#2563eb]"
            >
              <option value="">Select a user...</option>

              {availableUsers.map((user) => (
                <option key={user.id} value={String(user.id)}>
                  {user.name}
                  {user.email ? ` (${user.email})` : ''}
                </option>
              ))}
            </select>

            {availableUsers.length === 0 && (
              <p className="mt-2 text-sm text-[#6b7280]">
                There are no other users available to add.
              </p>
            )}

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                disabled={isAddingMember}
                onClick={() => setAddMemberOpen(false)}
                className="rounded-lg border border-[#d1d5db] px-4 py-2 text-sm font-medium text-[#374151] hover:bg-[#f9fafb] disabled:opacity-50"
              >
                Cancel
              </button>

              <button
                type="button"
                disabled={
                  !selectedUserId ||
                  isAddingMember ||
                  availableUsers.length === 0
                }
                onClick={handleAddParticipant}
                className="rounded-lg bg-[#2563eb] px-4 py-2 text-sm font-medium text-white hover:bg-[#1d4ed8] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isAddingMember ? 'Adding...' : 'Add member'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* IZMENJENO: filter prima samo članove projekta */}
      <FilterBar filters={filters} onChange={setFilters} users={members} />

      <DndContext
        sensors={sensors}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
        onDragCancel={() => setActiveId(null)}
      >
        <div className="flex items-start gap-4 overflow-x-auto pb-4">
          {columns.map((column) => (
            <BoardColumn
              key={column.id}
              id={column.id}
              title={column.title}
              tasks={visibleTasks.filter(
                (task) => task.status === column.id,
              )}
              onOpen={setSelectedId}
            />
          ))}
        </div>

        <DragOverlay dropAnimation={null}>
          {activeTask ? (
            <TaskCardView
              task={activeTask}
              className="rotate-2 cursor-grabbing shadow-xl ring-1 ring-black/5"
            />
          ) : null}
        </DragOverlay>
      </DndContext>

      {/* IZMENJENO: assignee se bira samo među članovima projekta */}
      <CreateTaskDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        projectId={id}
        users={members}
      />

      <TaskDetailDialog
        task={selectedTask}
        users={members}
        onClose={() => setSelectedId(null)}
      />
    </div>
  )
}