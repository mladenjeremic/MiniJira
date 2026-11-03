import type { ReactNode } from 'react'
import { SquareKanban } from 'lucide-react'

interface AuthCardProps {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}

// Zajednicki izgled za Login i Register stranicu.
export default function AuthCard({
  title,
  subtitle,
  children,
  footer,
}: AuthCardProps) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f8f9fb] px-4 py-10">
      <div className="w-full max-w-[420px] rounded-2xl border border-[#e6e8ec] bg-white p-8 shadow-[0_8px_30px_rgba(17,24,39,0.06)]">
        <div className="mx-auto flex h-11 w-11 items-center justify-center rounded-xl bg-[#eceef2] text-[#111827]">
          <SquareKanban size={20} />
        </div>

        <h1 className="mt-4 text-center text-lg font-semibold text-[#111827]">
          {title}
        </h1>

        <p className="mt-1.5 text-center text-sm text-[#6b7280]">{subtitle}</p>

        <div className="mt-6">{children}</div>

        <p className="mt-5 text-center text-sm text-[#6b7280]">{footer}</p>
      </div>
    </main>
  )
}