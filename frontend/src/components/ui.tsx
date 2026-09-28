import type { ReactNode } from 'react'

export function Button({ children, onClick, disabled = false, type = 'button', variant = 'primary' }: { children: ReactNode; onClick?: () => void; disabled?: boolean; type?: 'button' | 'submit'; variant?: 'primary' | 'secondary' }) {
  return <button type={type} disabled={disabled} onClick={onClick} className={`rounded-lg px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${variant === 'primary' ? 'bg-slate-900 text-white hover:bg-slate-800' : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'}`}>{children}</button>
}
export function Card({ children, className = '' }: { children: ReactNode; className?: string }) { return <section className={`rounded-2xl border border-slate-200 bg-white shadow-sm ${className}`}>{children}</section> }
export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'success' | 'warning' | 'danger' }) {
  const cls = { neutral: 'bg-slate-100 text-slate-700', success: 'bg-emerald-50 text-emerald-700', warning: 'bg-amber-50 text-amber-700', danger: 'bg-rose-50 text-rose-700' }[tone]
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${cls}`}>{children}</span>
}
