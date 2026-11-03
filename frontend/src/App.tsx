import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'

import AppLayout from '@/layouts/AppLayout'
import Backlog from '@/pages/BackLog'
import Board from '@/pages/Board'
import ProjectHistory from '@/pages/History' // NOVO
import Login from '@/pages/Login'
import Projects from '@/pages/Project'
import Register from '@/pages/Register'
import ProtectedRoute from './components/ui/ProtectedRoute'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Sve ispod zahteva prijavu; neulogovani idu na /login */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/projects" element={<Projects />} />
            <Route path="/projects/:id" element={<Board />} />
            <Route path="/projects/:id/backlog" element={<Backlog />} />
            <Route path="/projects/:id/history" element={<ProjectHistory />} /> {/* NOVO */}
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/projects" replace />} />
      </Routes>

      {/* Obavestenja (create task, drag & drop...) */}
      <Toaster position="bottom-right" richColors />
    </BrowserRouter>
  )
}