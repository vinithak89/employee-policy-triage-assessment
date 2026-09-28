import { useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  AlertTriangle,
  CheckCircle2,
  Download,
  FileText,
  ShieldAlert,
  Trash2,
  UploadCloud,
} from 'lucide-react'
import { processBatch, callerContext } from '../lib/api'
import { Badge, Button, Card } from '../components/ui'
import type { BatchResponse, BatchResult } from '../types/api'

function tone(status: string) {
  if (status === 'FAILED') return 'danger' as const
  if (status === 'COMPLETED') return 'success' as const
  return 'neutral' as const
}

function decisionTone(status?: string) {
  if (status === 'REVIEW_REQUIRED') return 'warning' as const
  if (status === 'APPROVED') return 'success' as const
  if (status === 'REJECTED') return 'danger' as const
  return 'neutral' as const
}

function downloadJson(data: unknown, name: string) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.click()
  URL.revokeObjectURL(url)
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function evidenceText(value: string | string[]) {
  return Array.isArray(value) ? value.join(' ') : value
}

export function BatchPage() {
  const input = useRef<HTMLInputElement>(null)
  const [caller, setCaller] = useState('atlas-employee-01')
  const [asOf, setAsOf] = useState('2026-09-21')
  const [files, setFiles] = useState<File[]>([])
  const [result, setResult] = useState<BatchResponse | null>(null)
  const [selected, setSelected] = useState<BatchResult | null>(null)
  const mutation = useMutation({
    mutationFn: () => processBatch(caller, asOf, files),
    onSuccess: data => {
      setResult(data)
      setSelected(data.results[0] ?? null)
    },
  })

  const context = callerContext(caller)

  function handleFileSelect(event: ChangeEvent<HTMLInputElement>) {
    const selectedFiles = Array.from(event.target.files ?? [])

    setFiles(previousFiles => {
      const existingKeys = new Set(previousFiles.map(file => `${file.name}|${file.size}|${file.lastModified}`))
      const newFiles = selectedFiles.filter(file => {
        const key = `${file.name}|${file.size}|${file.lastModified}`
        return !existingKeys.has(key)
      })
      return [...previousFiles, ...newFiles]
    })

    // Allows the user to select the same file again after removing it.
    event.target.value = ''
  }

  function removeFile(fileToRemove: File) {
    setFiles(previousFiles =>
      previousFiles.filter(
        file => !(file.name === fileToRemove.name && file.size === fileToRemove.size && file.lastModified === fileToRemove.lastModified),
      ),
    )
  }

  function clearFiles() {
    setFiles([])
    setResult(null)
    setSelected(null)
    if (input.current) input.current.value = ''
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Batch assessment</p>
        <div className="mt-1 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">Submit policy requests for review</h2>
            <p className="mt-1 text-sm text-slate-500">Upload one or more request documents and inspect the policy assessment returned by the AI service.</p>
          </div>
          <div className="flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-600">
            <span>{context.tenant}</span>
            <span className="text-slate-300">•</span>
            <span>{context.role}</span>
          </div>
        </div>
      </div>

      <Card className="p-6">
        <div className="grid gap-4 md:grid-cols-[1fr_1fr_auto]">
          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-600">Caller</label>
            <select value={caller} onChange={e => setCaller(e.target.value)} className="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-slate-400">
              <option value="atlas-employee-01">Atlas · Employee</option>
              <option value="atlas-contractor-01">Atlas · Contractor</option>
              <option value="boreal-employee-01">Boreal · Employee</option>
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-600">Policy as of</label>
            <input type="date" value={asOf} onChange={e => setAsOf(e.target.value)} className="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-slate-400" />
          </div>
          <div className="flex items-end">
            <Button onClick={() => mutation.mutate()} disabled={!files.length || mutation.isPending}>
              {mutation.isPending ? 'Processing…' : `Process ${files.length ? `${files.length} file${files.length === 1 ? '' : 's'}` : 'batch'}`}
            </Button>
          </div>
        </div>
      </Card>

      <Card className="p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h3 className="font-semibold text-slate-900">Request documents</h3>
            <p className="mt-1 text-xs text-slate-500">You can add files in multiple picker actions. Existing selections are preserved.</p>
          </div>
          {files.length > 0 && (
            <Button variant="secondary" onClick={clearFiles}>
              Clear all
            </Button>
          )}
        </div>

        <button
          type="button"
          onClick={() => input.current?.click()}
          className="mt-4 w-full rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 p-8 text-center transition hover:border-slate-300 hover:bg-white"
        >
          <UploadCloud className="mx-auto mb-2 text-slate-500" size={24} />
          <p className="text-sm font-semibold text-slate-700">Add TXT or text-based PDF files</p>
          <p className="mt-1 text-xs text-slate-500">Choose request-01 through request-08, or your own synthetic examples.</p>
        </button>
        <input ref={input} type="file" multiple accept=".txt,.pdf,text/plain,application/pdf" className="hidden" onChange={handleFileSelect} />

        {files.length > 0 ? (
          <div className="mt-4 space-y-2">
            <div className="flex items-center justify-between text-xs font-semibold text-slate-500">
              <span>{files.length} selected</span>
              <span>All selected files will be sent together</span>
            </div>
            {files.map(file => (
              <div key={`${file.name}|${file.size}|${file.lastModified}`} className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white px-3 py-2.5">
                <FileText size={17} className="shrink-0 text-slate-500" />
                <span className="min-w-0 flex-1 truncate text-sm font-medium text-slate-700">{file.name}</span>
                <span className="shrink-0 text-xs text-slate-400">{formatBytes(file.size)}</span>
                <button type="button" onClick={() => removeFile(file)} className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600" aria-label={`Remove ${file.name}`} title={`Remove ${file.name}`}>
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="mt-4 rounded-xl bg-slate-50 px-4 py-3 text-center text-xs text-slate-400">No documents selected yet.</div>
        )}

        {mutation.error && <p className="mt-3 text-xs text-rose-600">{(mutation.error as Error).message}</p>}
      </Card>

      {result && (
        <>
          <div className="grid gap-3 sm:grid-cols-3">
            {[
              ['Total', result.summary.total],
              ['Completed', result.summary.completed],
              ['Failed', result.summary.failed],
            ].map(([label, value]) => (
              <Card key={label as string} className="p-4">
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</p>
                <p className="mt-1 text-2xl font-bold text-slate-900">{value}</p>
              </Card>
            ))}
          </div>

          <Card className="overflow-hidden">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 p-5">
              <div>
                <h2 className="font-semibold text-slate-900">Batch report</h2>
                <p className="mt-1 text-xs text-slate-500">Batch {result.batch_id} · Every submitted request remains subject to human review.</p>
              </div>
              <Button variant="secondary" onClick={() => downloadJson(result, `${result.batch_id}.json`)}>
                <span className="flex items-center gap-2"><Download size={15} />Download JSON</span>
              </Button>
            </div>

            <div className="grid lg:grid-cols-[minmax(280px,0.8fr)_minmax(0,1.2fr)]">
              <div className="divide-y divide-slate-100">
                {result.results.map(item => (
                  <button key={item.document_id} type="button" onClick={() => setSelected(item)} className={`w-full px-5 py-4 text-left transition hover:bg-slate-50 ${selected?.document_id === item.document_id ? 'bg-slate-50' : ''}`}>
                    <div className="flex items-center gap-2">
                      <span className="min-w-0 flex-1 truncate text-sm font-semibold text-slate-800">{item.document_id}</span>
                      <Badge tone={tone(item.processing_status)}>{item.processing_status}</Badge>
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {item.decision?.status && <Badge tone={decisionTone(item.decision.status)}>{item.decision.status}</Badge>}
                      {item.security?.prompt_injection_detected && <Badge tone="danger">Security flag</Badge>}
                    </div>
                    <p className="mt-2 line-clamp-2 text-xs text-slate-500">{item.extracted?.benefit ?? item.error?.message ?? (item.duplicate_of ? `Duplicate of ${item.duplicate_of}` : 'No extracted fields')}</p>
                  </button>
                ))}
              </div>

              <div className="border-t border-slate-200 p-5 lg:border-l lg:border-t-0">
                {selected ? (
                  <div>
                    <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Document</p>
                        <h3 className="mt-1 text-lg font-bold text-slate-900">{selected.document_id}</h3>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {selected.decision?.status && <Badge tone={decisionTone(selected.decision.status)}>{selected.decision.status}</Badge>}
                        {selected.security?.prompt_injection_detected && <Badge tone="danger"><span className="flex items-center gap-1"><ShieldAlert size={13} />Injection detected</span></Badge>}
                      </div>
                    </div>

                    <div className="grid gap-3 sm:grid-cols-2">
                      {[
                        ['Benefit', selected.extracted?.benefit],
                        ['Reference', selected.extracted?.reference],
                        ['Amount', selected.extracted?.amount != null ? `${selected.extracted.currency ?? ''} ${selected.extracted.amount}`.trim() : 'Unavailable'],
                        ['Policy', String(selected.policy?.policy_id ?? 'Not determined')],
                      ].map(([key, value]) => (
                        <div key={key as string} className="rounded-xl bg-slate-50 p-3">
                          <dt className="text-xs text-slate-400">{key}</dt>
                          <dd className="mt-1 text-sm font-medium text-slate-700">{String(value ?? '—')}</dd>
                        </div>
                      ))}
                    </div>

                    {selected.policy && (
                      <div className="mt-5 rounded-xl border border-slate-200 p-4">
                        <div className="flex items-center justify-between gap-3">
                          <h4 className="text-sm font-semibold text-slate-900">Applicable policy</h4>
                          {selected.policy.match && <Badge tone="success"><span className="flex items-center gap-1"><CheckCircle2 size={13} />Match</span></Badge>}
                        </div>
                        <p className="mt-3 text-sm leading-6 text-slate-700">{selected.policy.text ?? 'No policy text returned.'}</p>
                        <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
                          {selected.policy.effective_from && <span className="rounded-full bg-slate-100 px-2.5 py-1">From {selected.policy.effective_from}</span>}
                          {selected.policy.effective_to && <span className="rounded-full bg-slate-100 px-2.5 py-1">To {selected.policy.effective_to}</span>}
                          {selected.policy.approval_status && <span className="rounded-full bg-slate-100 px-2.5 py-1">{selected.policy.approval_status}</span>}
                        </div>
                      </div>
                    )}

                    <div className="mt-5">
                      <h4 className="mb-2 text-sm font-semibold text-slate-900">Review findings</h4>
                      <div className="space-y-2">
                        {(selected.issues ?? selected.decision?.issues ?? []).map((issue, i) => (
                          <div key={i} className="flex gap-2 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
                            <AlertTriangle size={14} className="mt-0.5 shrink-0" />
                            <span>{issue}</span>
                          </div>
                        ))}
                        {selected.duplicate_of && (
                          <div className="rounded-lg bg-slate-50 p-3 text-xs text-slate-600">Duplicate of <strong>{selected.duplicate_of}</strong></div>
                        )}
                        {!selected.issues?.length && !selected.decision?.issues?.length && !selected.duplicate_of && <p className="text-sm text-slate-500">No additional findings.</p>}
                      </div>
                    </div>

                    {selected.extracted?.field_evidence && Object.keys(selected.extracted.field_evidence).length > 0 && (
                      <div className="mt-5">
                        <h4 className="mb-2 text-sm font-semibold text-slate-900">Extraction evidence</h4>
                        <div className="space-y-2">
                          {Object.entries(selected.extracted.field_evidence).map(([field, value]) => (
                            <div key={field} className="rounded-lg border border-slate-200 p-3">
                              <p className="text-xs font-semibold capitalize text-slate-500">{field.replace(/_/g, ' ')}</p>
                              <p className="mt-1 text-xs leading-5 text-slate-600">{evidenceText(value)}</p>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {selected.error && (
                      <div className="mt-5 rounded-xl bg-rose-50 p-4 text-sm text-rose-800">
                        <p className="font-semibold">Processing error</p>
                        <p className="mt-1">{selected.error.message ?? selected.error.code ?? 'Unknown error'}</p>
                      </div>
                    )}
                  </div>
                ) : (
                  <p className="text-sm text-slate-400">Select a document to inspect its report.</p>
                )}
              </div>
            </div>
          </Card>
        </>
      )}
    </div>
  )
}
