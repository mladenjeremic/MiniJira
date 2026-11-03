import { useParams } from 'react-router-dom'

import ProjectHeader from '@/components/ui/ProjectHeader'
import { UserAvatar } from '@/components/ui/task-ui'
import { useProjects } from '@/context/ProjectsContext'
import { useProjectHistory } from '@/hooks/useProjectHistory'
import { formatDateTime } from '@/lib/format'
import NoProjectAccess from '@/components/ui/NoProjectAccess'

export default function ProjectHistory() {
  const { id = '' } = useParams()
  const { projects, isLoading: projectsLoading } = useProjects()

  const project = projects.find((item) => item.id === id)
  const { items, isLoading, hasError } = useProjectHistory(id, Boolean(project))

  if (!projectsLoading && !project) {
    return <NoProjectAccess />
  }

  return (
    <div className="space-y-5">
      <ProjectHeader project={project} projectId={id} view="history" />

      {isLoading ? (
        <p className="py-12 text-center text-sm text-[#6b7280]">
          Loading history...
        </p>
      ) : hasError ? (
        <p className="py-12 text-center text-sm text-red-600">
          Could not load the project history.
        </p>
      ) : items.length === 0 ? (
        <p className="rounded-xl border border-dashed border-[#d9dde4] bg-white px-6 py-12 text-center text-sm text-[#6b7280]">
          No activity yet.
        </p>
      ) : (
        <ol className="divide-y divide-[#f0f1f4] rounded-xl border border-[#e6e8ec] bg-white shadow-sm">
          {items.map((item) => (
            <li key={item.id} className="flex items-start gap-3 px-4 py-3">
              <UserAvatar name={item.actor?.name ?? ''} />

              <div className="min-w-0 flex-1">
                <p className="break-words text-sm text-[#111827]">
                  {item.message}
                </p>
                <p className="mt-0.5 text-xs text-[#6b7280]">
                  {formatDateTime(item.createdAt)}
                </p>
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}