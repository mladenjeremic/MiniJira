import { ChevronDown } from 'lucide-react'

export interface SelectOption {
  value: string
  label: string
}

interface SelectFieldProps {
  value: string
  onChange: (value: string) => void
  options: SelectOption[]
  id?: string
  ariaLabel?: string
  className?: string
  compact?: boolean
  disabled?: boolean
}

// Obican <select> sa strelicom, stilizovan kao na slikama iz zadatka.
export default function SelectField({
  value,
  onChange,
  options,
  id,
  ariaLabel,
  className = '',
  compact = false,
  disabled = false,
}: SelectFieldProps) {
  return (
    <div className={`relative ${className}`}>
      <select
        id={id}
        aria-label={ariaLabel}
        disabled={disabled}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className={`w-full appearance-none rounded-lg border border-[#e2e5ea] bg-white pl-3 pr-9 text-sm text-[#111827] shadow-sm outline-none focus:border-[#9aa6ba] focus:ring-2 focus:ring-[#dfe5ee] disabled:opacity-60 ${
          compact ? 'h-9' : 'h-10'
        }`}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      <ChevronDown
        size={15}
        className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[#6b7280]"
      />
    </div>
  )
}