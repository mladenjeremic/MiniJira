import { Navigate, Outlet } from 'react-router-dom'

import { ProjectsProvider } from '@/context/ProjectsContext'
import { TasksProvider } from '@/context/TasksContext'
import { isLoggedIn } from '@/lib/session'

// Neulogovani korisnici uvek idu na login stranicu.
export default function ProtectedRoute() {
  if (!isLoggedIn()) {
    return <Navigate to="/login" replace />
  }

  return (
    <ProjectsProvider>
      <TasksProvider>
        <Outlet />
      </TasksProvider>
    </ProjectsProvider>
  )
}