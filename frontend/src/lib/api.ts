import type { AnswerResponse, BatchResponse } from '../types/api'

const API = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export type CallerContext = {
  tenant: string
  role: string
}

export function callerContext(callerId: string): CallerContext {
  if (callerId.startsWith('boreal-')) {
    return { tenant: 'Boreal', role: callerId.includes('contractor') ? 'contractor' : 'employee' }
  }
  return { tenant: 'Atlas', role: callerId.includes('contractor') ? 'contractor' : 'employee' }
}

export async function askPolicy(callerId: string, payload: { question: string; as_of: string }): Promise<AnswerResponse> {
  const { tenant, role } = callerContext(callerId)
  const response = await fetch(`${API}/answer`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Caller-Id': callerId,
      'X-Caller-Tenant': tenant,
      'X-Caller-Role': role,
    },
    body: JSON.stringify(payload),
  })
  if (!response.ok) throw new Error(await response.text())
  return response.json()
}

function documentIdFromFilename(filename: string) {
  return filename.replace(/\.[^/.]+$/, '')
}

export async function processBatch(callerId: string, asOf: string, files: File[]): Promise<BatchResponse> {
  const { tenant, role } = callerContext(callerId)
  const metadata = {
    batch_id: `ui-${new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)}`,
    as_of: asOf,
    documents: files.map(file => ({
      document_id: documentIdFromFilename(file.name),
      filename: file.name,
    })),
  }

  const form = new FormData()
  // The Spring API forwards this as the Python `metadata: str = Form(...)` field.
  form.append('metadata', JSON.stringify(metadata))
  files.forEach(file => form.append('files', file, file.name))

  const response = await fetch(`${API}/batches`, {
    method: 'POST',
    headers: {
      'X-Caller-Id': callerId,
      'X-Caller-Tenant': tenant,
      'X-Caller-Role': role,
    },
    body: form,
  })
  if (!response.ok) throw new Error(await response.text())
  return response.json()
}
