import { Link } from 'react-router-dom'
import { History as HistoryIcon, ListTodo, Plus, SquareKanban } from 'lucide-react'

import type { Project } from '@/context/ProjectsContext'
import { btnPrimary } from './task-ui'

interface ProjectHeaderProps {
  project: Project | undefined
  projectId: string
  view: 'board' | 'backlog' | 'history'
  onNewTask?: () => void
}

function toggleClass(active: boolean) {
  return `inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium transition ${
    active
      ? 'bg-[#f1f2f5] text-[#111827] shadow-sm'
      : 'text-[#6b7280] hover:text-[#111827]'
  }`
}

export default function ProjectHeader({
  project,
  projectId,
  view,
  onNewTask,
}: ProjectHeaderProps) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p className="text-xs text-[#6b7280]">
          <Link to="/projects" className="hover:text-[#111827]">
            Projects
          </Link>{' '}
          / {project?.key ?? '...'}
        </p>

        <h1 className="mt-1 text-2xl font-bold tracking-tight text-[#111827]">
          {project ? project.name : 'Loading...'}
          {view === 'backlog' ? ' — Backlog' : ''}
          {view === 'history' ? ' — History' : ''}
        </h1>
      </div>

      <div className="flex items-center gap-3">
        <div className="inline-flex rounded-lg border border-[#e2e5ea] bg-white p-0.5 shadow-sm">
          <Link to={`/projects/${projectId}`} className={toggleClass(view === 'board')}>
            <SquareKanban size={14} />
            Board
          </Link>

          <Link
            to={`/projects/${projectId}/backlog`}
            className={toggleClass(view === 'backlog')}
          >
            <ListTodo size={14} />
            Backlog
          </Link>

          <Link
            to={`/projects/${projectId}/history`}
            className={toggleClass(view === 'history')}
          >
            <HistoryIcon size={14} />
            History
          </Link>
        </div>

        {onNewTask && (
          <button type="button" onClick={onNewTask} className={btnPrimary}>
            <Plus size={16} />
            New task
          </button>
        )}
      </div>
    </div>
  )
}