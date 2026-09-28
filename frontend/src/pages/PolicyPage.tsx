import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { AlertCircle, CheckCircle2, Search } from 'lucide-react'
import { askPolicy } from '../lib/api'
import { Badge, Button, Card } from '../components/ui'
import type { AnswerResponse } from '../types/api'

export function PolicyPage() {
  const [caller, setCaller] = useState('atlas-employee-01')
  const [asOf, setAsOf] = useState('2026-09-21')
  const [question, setQuestion] = useState('What is my annual certification reimbursement limit?')
  const [result, setResult] = useState<AnswerResponse | null>(null)
  const mutation = useMutation({ mutationFn: () => askPolicy(caller, { question, as_of: asOf }), onSuccess: setResult })

  return <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
    <Card className="p-6">
      <div className="mb-5 flex items-center gap-3"><div className="rounded-xl bg-slate-900 p-2 text-white"><Search size={18} /></div><div><h2 className="font-semibold">Policy question</h2><p className="text-xs text-slate-500">Ask against eligible policy evidence.</p></div></div>
      <label className="mb-1 block text-xs font-semibold text-slate-600">Caller</label>
      <select value={caller} onChange={e => setCaller(e.target.value)} className="mb-4 w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm"><option>atlas-employee-01</option><option>atlas-contractor-01</option><option>boreal-employee-01</option></select>
      <label className="mb-1 block text-xs font-semibold text-slate-600">As of</label>
      <input type="date" value={asOf} onChange={e => setAsOf(e.target.value)} className="mb-4 w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm" />
      <label className="mb-1 block text-xs font-semibold text-slate-600">Question</label>
      <textarea rows={5} value={question} onChange={e => setQuestion(e.target.value)} className="mb-4 w-full resize-none rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-slate-400" />
      <Button onClick={() => mutation.mutate()} disabled={mutation.isPending || !question.trim()}>{mutation.isPending ? 'Checking…' : 'Ask policy'}</Button>
      {mutation.error && <p className="mt-3 text-xs text-rose-600">{(mutation.error as Error).message}</p>}
    </Card>
    <Card className="min-h-[420px] p-6">
      {!result ? <div className="flex h-full min-h-[360px] items-center justify-center text-center text-slate-400"><div><Search className="mx-auto mb-3" /><p className="text-sm">Your policy answer and evidence will appear here.</p></div></div> : <div>
        <div className="mb-6 flex items-start justify-between gap-4"><div><p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Business outcome</p><h2 className="mt-1 text-2xl font-bold text-slate-900">{result.status}</h2></div><Badge tone={result.status === 'ANSWERED' ? 'success' : result.status === 'CONFLICT' ? 'warning' : 'neutral'}>{result.status}</Badge></div>
        {result.answer ? <div className="mb-6 rounded-xl bg-slate-50 p-5 text-sm leading-6 text-slate-700">{result.answer}</div> : <div className="mb-6 rounded-xl bg-amber-50 p-5 text-sm text-amber-800"><AlertCircle className="mb-2" size={18} />No supported single answer was returned. Review the eligible evidence below.</div>}
        <h3 className="mb-3 text-sm font-semibold text-slate-900">Evidence</h3>
        <div className="space-y-3">{result.citations.length ? result.citations.map((c, i) => <div key={i} className="rounded-xl border border-slate-200 p-4"><div className="mb-2 flex items-center gap-2"><CheckCircle2 size={15} className="text-emerald-600" /><span className="text-xs font-semibold text-slate-500">{c.chunk_id}</span></div><p className="text-sm leading-6 text-slate-700">“{c.quote}”</p></div>) : <p className="text-sm text-slate-500">No citations returned.</p>}</div>
      </div>}
    </Card>
  </div>
}
