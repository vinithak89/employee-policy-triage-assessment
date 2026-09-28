# Production Design Note

The local design separates a Spring Boot API gateway from a Python policy/AI service. In production, Spring would authenticate callers with the enterprise identity provider, resolve tenant and role from trusted identity/authorization data, validate the request, assign a correlation ID, and enforce request/file limits. The Python service would receive only trusted caller context over mutually authenticated service-to-service transport.

Policy data should move from repository JSON to a versioned policy service or governed store with immutable IDs, approval state, effective intervals, tenant/role constraints, and audit history. Retrieval should first apply these metadata filters and only then use a hybrid lexical/vector index. Retrieved chunks should carry policy ID and source version so every generated statement is traceable to approved evidence. A real LLM provider can replace the offline double behind the same provider interface, with bounded timeouts, one generation attempt, schema validation, and circuit breaking.

Batch processing can remain synchronous for the assessment-sized workload, but production volume should use durable object storage and a queue/workflow with idempotency keys, per-document status, retries for transient infrastructure failures, and dead-letter handling. Duplicate detection should use content hashes plus a persisted idempotency record.

Security controls should include prompt-injection isolation, strict allow-listed tool access, output validation, secret redaction, malware/file-type scanning, size limits, and structured audit logs. Do not log full documents by default. Monitoring should measure extraction failures, retrieval misses, conflicts, provider failures, latency, and human-review rates.

Human review remains mandatory for reimbursement decisions. The system should provide evidence and review reasons but must not initiate payment or communicate an approval to an employee without an independent governed workflow.
