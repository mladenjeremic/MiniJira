import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Folder, LayoutGrid, LogOut, Menu, SquareKanban, X } from 'lucide-react'

import { useProjects } from '@/context/ProjectsContext'
import { getInitials } from '@/lib/format'
import { clearSession, getStoredUser } from '@/lib/session'

function navClass(isActive: boolean) {
  return `flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors ${
    isActive
      ? 'bg-[#f1f2f5] font-medium text-[#111827]'
      : 'text-[#4b5563] hover:bg-[#f6f7f9] hover:text-[#111827]'
  }`
}

export default function AppLayout() {
  const navigate = useNavigate()
  const { projects } = useProjects()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const user = getStoredUser()
  const userName = user?.name || user?.email?.split('@')[0] || 'My account'
  const firstName = userName.split(' ')[0]

  function handleLogout() {
    clearSession()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-[#fafafb] text-[#111827]">
      {/* Zatamnjenje pozadine na malim ekranima */}
      {sidebarOpen && (
        <button
          type="button"
          aria-label="Close navigation"
          className="fixed inset-0 z-30 bg-black/30 md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-60 flex-col border-r border-[#e9ebef] bg-white transition-transform duration-200 md:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-14 items-center justify-between px-5">
          <Link
            to="/projects"
            className="flex items-center gap-2"
            onClick={() => setSidebarOpen(false)}
          >
            <SquareKanban size={18} />
            <span className="text-sm font-bold">MiniJira</span>
          </Link>

          <button
            type="button"
            aria-label="Close sidebar"
            className="rounded-md p-1.5 text-[#6b7280] hover:bg-[#f3f4f6] md:hidden"
            onClick={() => setSidebarOpen(false)}
          >
            <X size={18} />
          </button>
        </div>

        <nav className="flex-1 space-y-5 overflow-y-auto px-3 py-3">
          <NavLink
            to="/projects"
            end
            onClick={() => setSidebarOpen(false)}
            className={({ isActive }) => navClass(isActive)}
          >
            <LayoutGrid size={16} />
            Projects
          </NavLink>

          <div>
            <p className="mb-1.5 px-3 text-[11px] font-semibold uppercase tracking-wider text-[#8b93a1]">
              Your projects
            </p>

            <div className="space-y-0.5">
              {projects.map((project) => (
                <NavLink
                  key={project.id}
                  to={`/projects/${project.id}`}
                  onClick={() => setSidebarOpen(false)}
                  className={({ isActive }) => navClass(isActive)}
                >
                  <Folder size={16} className="shrink-0 text-[#6b7280]" />

                  <span className="min-w-0 flex-1 truncate">{project.name}</span>

                  <span className="font-mono text-[11px] text-[#9aa1ad]">
                    {project.key}
                  </span>
                </NavLink>
              ))}

              {projects.length === 0 && (
                <p className="px-3 py-2 text-xs text-[#9aa1ad]">
                  No projects yet.
                </p>
              )}
            </div>
          </div>
        </nav>
      </aside>

      {/* Glavni sadrzaj */}
      <div className="min-h-screen md:ml-60">
        <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b border-[#e9ebef] bg-white px-4 sm:px-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              aria-label="Open navigation"
              className="rounded-md p-1.5 text-[#6b7280] hover:bg-[#f3f4f6] md:hidden"
              onClick={() => setSidebarOpen(true)}
            >
              <Menu size={19} />
            </button>

            <p className="text-sm text-[#6b7280]">Welcome back, {firstName}</p>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-[#eceef2] text-[11px] font-semibold text-[#374151]">
                {getInitials(userName)}
              </span>

              <span className="hidden max-w-40 truncate text-sm font-medium sm:inline">
                {userName}
              </span>
            </div>

            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex h-8 items-center gap-1.5 rounded-lg px-2.5 text-sm text-[#6b7280] transition hover:bg-[#f3f4f6] hover:text-[#111827]"
            >
              <LogOut size={15} />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </header>

        <main className="mx-auto w-full max-w-[1240px] p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
