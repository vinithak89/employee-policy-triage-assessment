export type Citation = { chunk_id: string; quote: string }
export type AnswerResponse = { status: string; answer: string | null; citations: Citation[] }

export type FieldEvidenceValue = string | string[]

export type BatchResult = {
  document_id: string
  processing_status: string
  extracted?: {
    benefit?: string | null
    amount?: number | null
    currency?: string | null
    reference?: string | null
    field_evidence?: Record<string, FieldEvidenceValue>
  } | null
  policy?: {
    match?: boolean
    conflict?: boolean
    policy_id?: string | null
    tenant?: string | null
    role?: string | null
    effective_from?: string | null
    effective_to?: string | null
    approval_status?: string | null
    text?: string | null
    amount_exceeds_limit?: boolean
    currency_matches?: boolean
    source?: Record<string, unknown> | null
    [key: string]: unknown
  } | null
  decision?: {
    status?: string
    review_required?: boolean
    issues?: string[]
    reason?: string | null
    policy_finding?: {
      outcome?: string
      policy_id?: string | null
      text?: string | null
    } | null
  } | null
  security?: { prompt_injection_detected?: boolean }
  review_required?: boolean
  issues?: string[]
  duplicate_of?: string | null
  error?: { code?: string; message?: string } | null
}

export type BatchResponse = {
  batch_id: string
  summary: { total: number; completed: number; failed: number }
  results: BatchResult[]
}
