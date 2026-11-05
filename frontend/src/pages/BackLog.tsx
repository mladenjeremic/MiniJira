import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react'
import { toast } from 'sonner'

import CreateTaskDialog from '@/components/CreateTaskDialog'

import { useProjects } from '@/context/ProjectsContext'
import { useTasks, type Priority, type Task } from '@/context/TasksContext'
import { useProjectMembers } from '@/hooks/useProjectMembers' // NOVO (zamenjuje useUsers)
import { formatDate } from '@/lib/format'
import { columns, KeyBadge, PriorityBadge, StatusBadge, TypeIcon, UserAvatar } from '@/components/ui/task-ui'
import FilterBar, { applyFilters, emptyFilters, type Filters } from '@/components/ui/FilterBar'
import ProjectHeader from '@/components/ui/ProjectHeader'
import TaskDetailDialog from '@/components/ui/TaskDetailDialog'
import NoProjectAccess from '@/components/ui/NoProjectAccess'

type SortField =
  | 'number'
  | 'title'
  | 'type'
  | 'status'
  | 'priority'
  | 'assignee'
  | 'createdAt'

type SortDirection = 'asc' | 'desc'

const tableColumns: { field: SortField; label: string }[] = [
  { field: 'number', label: 'Key' },
  { field: 'title', label: 'Title' },
  { field: 'type', label: 'Type' },
  { field: 'status', label: 'Status' },
  { field: 'priority', label: 'Priority' },
  { field: 'assignee', label: 'Assignee' },
  { field: 'createdAt', label: 'Created' },
]

const priorityRank: Record<Priority, number> = { Low: 1, Medium: 2, High: 3 }
const statusRank = columns.map((column) => column.id)

function compareTasks(a: Task, b: Task, field: SortField): number {
  switch (field) {
    case 'number':
      return a.number - b.number
    case 'title':
      return a.title.localeCompare(b.title)
    case 'type':
      return a.type.localeCompare(b.type)
    case 'status':
      return statusRank.indexOf(a.status) - statusRank.indexOf(b.status)
    case 'priority':
      return priorityRank[a.priority] - priorityRank[b.priority]
    case 'assignee':
      return a.assignee.localeCompare(b.assignee)
    case 'createdAt':
      return a.createdAt.localeCompare(b.createdAt)
  }
}

export default function Backlog() {
  const { id = '' } = useParams()
  const { projects, isLoading } = useProjects() // IZMENJENO: dodat isLoading
  const { tasks, loadTasks } = useTasks()

  const project = projects.find((item) => item.id === id)

  // NOVO: članovi projekta umesto svih korisnika
  const members = useProjectMembers(project)

  const [filters, setFilters] = useState<Filters>(emptyFilters)
  const [sortField, setSortField] = useState<SortField>('number')
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc')
  const [createOpen, setCreateOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  // IZMENJENO: taskove učitavamo tek kad znamo da projekat postoji i da imamo pristup
  const hasProject = Boolean(project)

  useEffect(() => {
    if (!hasProject) return

    loadTasks(id).catch(() => toast.error('Failed to load project tasks.'))
  }, [id, hasProject, loadTasks])

  const projectTasks = useMemo(
    () => tasks.filter((task) => task.projectId === id),
    [tasks, id],
  )

  const rows = useMemo(() => {
    const direction = sortDirection === 'asc' ? 1 : -1

    return applyFilters(projectTasks, filters).sort(
      (a, b) => compareTasks(a, b, sortField) * direction,
    )
  }, [projectTasks, filters, sortField, sortDirection])

  const selectedTask = selectedId
    ? (projectTasks.find((task) => task.id === selectedId) ?? null)
    : null

  function toggleSort(field: SortField) {
    if (field === sortField) {
      setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortField(field)
      setSortDirection('asc')
    }
  }

  // NOVO: projekat ne postoji ili korisnik nije vlasnik/učesnik
  if (!isLoading && !project) {
    return <NoProjectAccess />
  }

  return (
    <div className="space-y-5">
      <ProjectHeader
        project={project}
        projectId={id}
        view="backlog"
        onNewTask={() => setCreateOpen(true)}
      />

      <FilterBar filters={filters} onChange={setFilters} users={members} />

      <div className="overflow-x-auto rounded-xl border border-[#e6e8ec] bg-white shadow-sm">
        <table className="w-full min-w-[860px] border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-[#e6e8ec] bg-[#fafafb] text-xs font-semibold text-[#6b7280]">
              {tableColumns.map((column) => {
                const active = sortField === column.field

                return (
                  <th key={column.field} className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => toggleSort(column.field)}
                      className={`inline-flex items-center gap-1 hover:text-[#111827] ${
                        active ? 'text-[#111827]' : ''
                      }`}
                    >
                      {column.label}

                      {active ? (
                        sortDirection === 'asc' ? (
                          <ArrowUp size={12} />
                        ) : (
                          <ArrowDown size={12} />
                        )
                      ) : (
                        <ArrowUpDown size={12} className="text-[#b6bcc7]" />
                      )}
                    </button>
                  </th>
                )
              })}
            </tr>
          </thead>

          <tbody>
            {rows.map((task) => (
              <tr
                key={task.id}
                onClick={() => setSelectedId(task.id)}
                className="cursor-pointer border-b border-[#f0f1f4] last:border-b-0 even:bg-[#fafafb] hover:bg-[#f3f4f6]"
              >
                <td className="px-4 py-3">
                  <KeyBadge>{task.key}</KeyBadge>
                </td>

                <td className="max-w-[360px] px-4 py-3 font-semibold text-[#111827]">
                  {task.title}
                </td>

                <td className="px-4 py-3">
                  <span className="inline-flex items-center gap-1.5 text-[#374151]">
                    <TypeIcon type={task.type} size={14} />
                    {task.type}
                  </span>
                </td>

                <td className="px-4 py-3">
                  <StatusBadge status={task.status} />
                </td>

                <td className="px-4 py-3">
                  <PriorityBadge priority={task.priority} />
                </td>

                <td className="px-4 py-3">
                  <span className="inline-flex items-center gap-2 text-[#374151]">
                    <UserAvatar name={task.assignee} size="sm" />
                    {task.assignee || 'Unassigned'}
                  </span>
                </td>

                <td className="whitespace-nowrap px-4 py-3 text-[#6b7280]">
                  {formatDate(task.createdAt)}
                </td>
              </tr>
            ))}

            {rows.length === 0 && (
              <tr>
                <td
                  colSpan={tableColumns.length}
                  className="px-4 py-12 text-center text-sm text-[#6b7280]"
                >
                  No tasks found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

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