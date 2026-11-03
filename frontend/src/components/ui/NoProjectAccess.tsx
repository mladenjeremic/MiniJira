import { Link } from 'react-router-dom'
import { Lock } from 'lucide-react'

export default function NoProjectAccess() {
  return (
    <div className="rounded-xl border border-dashed border-[#d9dde4] bg-white px-6 py-16 text-center">
      <Lock className="mx-auto mb-3 text-[#9aa1ad]" size={28} />

      <h2 className="text-sm font-semibold">No access to this project</h2>

      <p className="mt-1 text-xs text-[#6b7280]">
        The project does not exist or the owner has not added you to it.
      </p>

      <Link
        to="/projects"
        className="mt-4 inline-block text-xs font-medium text-[#2563eb] hover:underline"
      >
        Back to projects
      </Link>
    </div>
  )
}