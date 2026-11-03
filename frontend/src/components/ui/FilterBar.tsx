import { Search, X } from 'lucide-react'


import type { Task } from '@/context/TasksContext'
import type { AppUser } from '@/hooks/useUsers'
import SelectField from './SelectField'
import { priorities, taskTypes } from './task-ui'

export type Filters = {
  search: string
  assignee: string // 'all' | 'unassigned' | id korisnika
  priority: string // 'all' | Low | Medium | High
  type: string // 'all' | Bug | Task | Story
}

export const emptyFilters: Filters = {
  search: '',
  assignee: 'all',
  priority: 'all',
  type: 'all',
}

export function hasActiveFilters(filters: Filters): boolean {
  return (
    filters.search.trim() !== '' ||
    filters.assignee !== 'all' ||
    filters.priority !== 'all' ||
    filters.type !== 'all'
  )
}

// Ista logika filtriranja koristi se i na Board-u i u Backlog tabeli.
export function applyFilters(tasks: Task[], filters: Filters): Task[] {
  const query = filters.search.trim().toLowerCase()

  return tasks.filter((task) => {
    if (
      query &&
      !task.title.toLowerCase().includes(query) &&
      !task.key.toLowerCase().includes(query)
    ) {
      return false
    }

    if (filters.assignee === 'unassigned' && task.assigneeId !== null) {
      return false
    }

    if (
      filters.assignee !== 'all' &&
      filters.assignee !== 'unassigned' &&
      String(task.assigneeId) !== filters.assignee
    ) {
      return false
    }

    if (filters.priority !== 'all' && task.priority !== filters.priority) {
      return false
    }

    if (filters.type !== 'all' && task.type !== filters.type) {
      return false
    }

    return true
  })
}

interface FilterBarProps {
  filters: Filters
  onChange: (filters: Filters) => void
  users: AppUser[]
}

export default function FilterBar({ filters, onChange, users }: FilterBarProps) {
  function update(changes: Partial<Filters>) {
    onChange({ ...filters, ...changes })
  }

  return (
    <div className="flex flex-wrap items-center gap-2.5">
      <div className="relative w-full sm:w-[300px]">
        <Search
          size={15}
          className="absolute left-3 top-1/2 -translate-y-1/2 text-[#9aa1ad]"
        />

        <input
          value={filters.search}
          onChange={(event) => update({ search: event.target.value })}
          placeholder="Search tasks..."
          aria-label="Search tasks"
          className="h-9 w-full rounded-lg border border-[#e2e5ea] bg-white pl-9 pr-3 text-sm text-[#111827] shadow-sm outline-none placeholder:text-[#9aa1ad] focus:border-[#9aa6ba] focus:ring-2 focus:ring-[#dfe5ee]"
        />
      </div>

      <SelectField
        compact
        className="w-[150px]"
        ariaLabel="Filter by assignee"
        value={filters.assignee}
        onChange={(value) => update({ assignee: value })}
        options={[
          { value: 'all', label: 'All assignees' },
          { value: 'unassigned', label: 'Unassigned' },
          ...users.map((user) => ({
            value: String(user.id),
            label: user.name,
          })),
        ]}
      />

      <SelectField
        compact
        className="w-[150px]"
        ariaLabel="Filter by priority"
        value={filters.priority}
        onChange={(value) => update({ priority: value })}
        options={[
          { value: 'all', label: 'All priorities' },
          ...priorities.map((priority) => ({
            value: priority,
            label: priority,
          })),
        ]}
      />

      <SelectField
        compact
        className="w-[130px]"
        ariaLabel="Filter by type"
        value={filters.type}
        onChange={(value) => update({ type: value })}
        options={[
          { value: 'all', label: 'All types' },
          ...taskTypes.map((type) => ({ value: type, label: type })),
        ]}
      />

      {hasActiveFilters(filters) && (
        <button
          type="button"
          onClick={() => onChange(emptyFilters)}
          className="inline-flex h-9 items-center gap-1 rounded-lg px-2.5 text-sm text-[#6b7280] hover:bg-[#eceef2] hover:text-[#111827]"
        >
          <X size={14} />
          Reset
        </button>
      )}
    </div>
  )
}