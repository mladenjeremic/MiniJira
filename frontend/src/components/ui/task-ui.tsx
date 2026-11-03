import { BookOpen, Bug, SquareCheckBig } from 'lucide-react'

import type { Priority, TaskStatus, TaskType } from '@/context/TasksContext'
import { getInitials } from '@/lib/format'

export const columns: { id: TaskStatus; title: string }[] = [
  { id: 'backlog', title: 'Backlog' },
  { id: 'todo', title: 'To Do' },
  { id: 'in-progress', title: 'In Progress' },
  { id: 'done', title: 'Done' },
]

export const statusLabels: Record<TaskStatus, string> = {
  backlog: 'Backlog',
  todo: 'To Do',
  'in-progress': 'In Progress',
  done: 'Done',
}

export const priorities: Priority[] = ['Low', 'Medium', 'High']
export const taskTypes: TaskType[] = ['Bug', 'Task', 'Story']

/* ---------- zajednicke CSS klase (forme i dugmad) ---------- */

export const labelClass =
  'mb-1.5 block text-sm font-semibold text-[#111827]'

export const fieldClass =
  'h-10 w-full rounded-lg border border-[#e2e5ea] bg-white px-3 text-sm text-[#111827] shadow-sm outline-none placeholder:text-[#9aa1ad] focus:border-[#9aa6ba] focus:ring-2 focus:ring-[#dfe5ee]'

export const textareaClass =
  'w-full resize-y rounded-lg border border-[#e2e5ea] bg-white px-3 py-2 text-sm text-[#111827] shadow-sm outline-none placeholder:text-[#9aa1ad] focus:border-[#9aa6ba] focus:ring-2 focus:ring-[#dfe5ee]'

const btnBase =
  'inline-flex h-9 items-center justify-center gap-2 rounded-lg px-4 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50'

export const btnPrimary = `${btnBase} bg-[#151c2e] text-white shadow-sm hover:bg-[#262f47]`
export const btnOutline = `${btnBase} border border-[#e2e5ea] bg-white text-[#111827] shadow-sm hover:bg-[#f6f7f9]`
export const btnDanger = `${btnBase} bg-[#d92d20] text-white shadow-sm hover:bg-[#b42318]`

/* ---------- male prezentacione komponente ---------- */

export function TypeIcon({
  type,
  size = 16,
}: {
  type: TaskType
  size?: number
}) {
  if (type === 'Bug') {
    return <Bug size={size} className="shrink-0 text-[#dc2626]" />
  }

  if (type === 'Story') {
    return <BookOpen size={size} className="shrink-0 text-[#16a34a]" />
  }

  return <SquareCheckBig size={size} className="shrink-0 text-[#4f46e5]" />
}

const priorityStyles: Record<Priority, string> = {
  Low: 'bg-[#eef0f4] text-[#4b5563]',
  Medium: 'bg-[#fdf1c7] text-[#92600a]',
  High: 'bg-[#fbdcdc] text-[#b42318]',
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium ${priorityStyles[priority]}`}
    >
      {priority}
    </span>
  )
} 

export function StatusBadge({ status }: { status: TaskStatus }) {
  return (
    <span className="inline-flex items-center rounded-full border border-[#d9dde4] bg-white px-2.5 py-0.5 text-xs font-medium text-[#111827]">
      {statusLabels[status]}
    </span>
  )
}

export function KeyBadge({ children }: { children: string }) {
  return (
    <span className="inline-flex items-center rounded-md border border-[#e2e5ea] bg-white px-1.5 py-0.5 font-mono text-[11px] text-[#374151]">
      {children}
    </span>
  )
}

export function UserAvatar({
  name,
  size = 'md',
}: {
  name: string
  size?: 'sm' | 'md'
}) {
  const sizeClass = size === 'sm' ? 'h-6 w-6 text-[10px]' : 'h-7 w-7 text-[10px]'

  if (!name) {
    return (
      <span
        title="Unassigned"
        className={`inline-flex shrink-0 items-center justify-center rounded-full border border-dashed border-[#c4c9d4] text-[#9aa1ad] ${sizeClass}`}
      >
        ?
      </span>
    )
  }

  return (
    <span
      title={name}
      className={`inline-flex shrink-0 items-center justify-center rounded-full bg-[#eceef2] font-semibold text-[#374151] ${sizeClass}`}
    >
      {getInitials(name)}
    </span>
  )
}