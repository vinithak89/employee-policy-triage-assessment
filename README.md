# Employee Policy and Reimbursement Triage

Local assessment implementation with a Spring Boot public API backed by a Python/FastAPI service.

The assessment core is intentionally offline/deterministic: no Azure OpenAI account, paid model,
personal API key, vector database, OCR, or production authentication is required.

An optional React/Vite frontend is included as a presentation layer for human-readable policy answers
and reimbursement batch review. It does not change the assessment API or processing logic.

## Architecture

```text
                           ┌─────────────────────────┐
                           │ React/Vite UI :5173     │
                           │ Policy + Batch Review   │
                           └────────────┬────────────┘
                                        │
                                        v
Client / Swagger / CLI  ───────> Spring Boot :8080
                                        │
                                        v
                                 Python/FastAPI :8001
                                        │
                       ┌────────────────┼────────────────┐
                       v                v                v
                 Extraction      Eligibility       Deterministic
                 TXT/PDF         + Retrieval        Model Double
                       │                │                │
                       └────────────────┼────────────────┘
                                        v
                              Supplied Policy Corpus
```

Spring owns caller validation and public endpoints. Python performs document extraction, eligibility
filtering, deterministic retrieval, evidence handling, and batch review reporting.

## Project Structure

```text
employee-policy-triage/
├── spring-api/              # Public Spring Boot API
├── python-ai-service/       # Python/FastAPI AI and document service
├── frontend/                # Optional React/Vite demonstration UI
├── sample-data/             # Supplied assessment requests and representative responses
├── docs/                    # Decision and production design notes
└── README.md
```

## Prerequisites

- Java 17
- Maven 3.9+ (or use the included Maven Wrapper)
- Python 3.12+
- Node.js 20+ and npm (only for the optional React UI)
- No external AI/API credential

## Quick Start

### 1. Start the Python service

Windows PowerShell:

```powershell
cd python-ai-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --port 8001
```

### 2. Start Spring Boot

Open another terminal:

```powershell
cd spring-api
./mvnw spring-boot:run
```

Windows CMD:

```cmd
cd spring-api
mvnw.cmd spring-boot:run
```

The public API is available at:

`http://localhost:8080`

### 3. Optional: start the React UI

Open a third terminal:

```powershell
cd frontend
npm install
npm run dev
```

The UI is available at:

`http://localhost:5173`

The UI is optional for the assessment backend and uses the same Spring Boot APIs exposed through Swagger.

## Service URLs

| Purpose | URL |
|---|---|
| React UI | `http://localhost:5173` |
| Spring Boot API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Python/FastAPI | `http://localhost:8001` |
| Python health | `http://localhost:8001/health` |

## Swagger / OpenAPI

Swagger UI is the recommended interactive way to test the public API.

Open:

`http://localhost:8080/swagger-ui/index.html`

OpenAPI JSON is available at:

`http://localhost:8080/v3/api-docs`

### Test `POST /answer` in Swagger

1. Start the Python service.
2. Start Spring Boot.
3. Open Swagger UI.
4. Expand `POST /answer`.
5. Click **Try it out**.
6. Set `X-Caller-Id` to:

   `atlas-employee-01`

7. Use this request body:

```json
{
  "question": "What is my annual certification reimbursement limit?",
  "as_of": "2026-09-21"
}
```

8. Click **Execute**.

A successful business outcome is returned as `ANSWERED`, `INSUFFICIENT_EVIDENCE`, or `CONFLICT`.

### Test `POST /batches` in Swagger

1. Expand `POST /batches`.
2. Click **Try it out**.
3. Set `X-Caller-Id` to `atlas-employee-01`.
4. Provide the JSON from `sample-data/batch-metadata.json` as the `metadata` part.
5. Upload the corresponding files from `sample-data/requests/`.
6. Click **Execute**.
7. Inspect the batch summary and document-level results.

Swagger is intended for quick interactive testing. The command-line example below remains available for
reproducible testing.

## Test the Application

Testing is divided into automated tests, API testing, and the end-user UI demonstration.

### 1. Python automated tests

```powershell
cd python-ai-service
python -m pytest -q
```

The Python suite covers policy outcomes, effective dates, tenant/role access, evidence quotations,
ambiguous input, exact duplicates, mixed success/failure, security isolation, and provider
timeout/unavailable/malformed-output doubles.

### 2. Spring Boot automated tests

```powershell
cd spring-api
./mvnw test
```

Windows CMD:

```cmd
cd spring-api
mvnw.cmd test
```

### 3. React UI demonstration

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

The UI provides two workflows:

- **Policy Assistant** — caller, date, question, business outcome, answer, and evidence.
- **Batch Review** — TXT/PDF upload, batch summary, document-level results, extracted fields, policy
  findings, review reasons, duplicate/failure indicators, and JSON report download.

The UI is a presentation layer; the backend JSON remains the source of the structured result.

## Public API

Both required endpoints are exposed through Spring Boot.

### POST /answer

Header:

`X-Caller-Id: atlas-employee-01`

Request:

```json
{
  "question": "What is my annual certification reimbursement limit?",
  "as_of": "2026-09-21"
}
```

Business outcomes are always one of:

- `ANSWERED`: answer plus eligible verbatim citations
- `INSUFFICIENT_EVIDENCE`: `answer` is null and citations are empty
- `CONFLICT`: `answer` is null and citations show the eligible disagreement

Citation shape:

```json
{
  "chunk_id": "atlas-cert-current",
  "quote": "The annual certification reimbursement limit for employees is INR 25000."
}
```

Missing/unknown caller is rejected. Tenant and role come only from the Spring caller registry; request
text cannot change them.

### POST /batches

Header:

`X-Caller-Id: atlas-employee-01`

Multipart parts:

- `metadata`: JSON containing `batch_id`, `as_of`, and unique `{document_id, filename}` entries.
- repeated `files` parts whose filenames exactly match the manifest.

Supported input files are UTF-8 `.txt` and text-based `.pdf`. Every item gets a result. Exact duplicates
are retained as separate results and linked with `duplicate_of`; one failure does not stop other items.

Every submitted request has `review_required: true`. The application never approves a claim, initiates
payment, or sends employee messages. A policy limit is not treated as remaining balance, eligibility,
or payable amount.

## Policy Corpus

`python-ai-service/policies/policies.json` is the single repository source of truth and contains the
twelve supplied assessment records with their IDs, metadata, and passage text. Eligibility filtering uses
tenant, permitted role, Approved state, and an inclusive-start/exclusive-end effective interval before
retrieval.

## Security Behavior

Submitted document text is untrusted. Prompt-injection phrases are detected for review, but cannot change
the authenticated caller, tenant, role, policy access, or decision behavior. Ineligible passages are
filtered before the offline provider is called.

## Technical Errors

Provider timeout, provider unavailable, and malformed provider output are technical failures and are not
converted into policy findings. The FastAPI service maps them to 504/503/502 respectively; business
outcomes remain the three assessment statuses above.

## Demonstration Data

- `sample-data/requests/` contains the supplied `request-01` through `request-08` documents.
- `sample-data/batch-metadata.json` is the mixed-batch manifest for `atlas-employee-01` at `2026-09-21`.
- `sample-data/responses/batch-demo-response.json` is a representative batch result.
- `sample-data/responses/answer-certification.json` is a representative `/answer` result.
- `docs/production-design-note.md` is the production design note.
- `docs/decision-note.md` records the implementation decisions.

## Assessment Scenarios to Verify

The supplied batch is intentionally designed to exercise the important assessment cases.

| Document | Scenario | What to verify |
|---|---|---|
| request-01 | Certification reimbursement | Applicable policy evidence is returned; human review remains required |
| request-02 | Text-based PDF | PDF text extraction works and policy findings are visible |
| request-03 | Ambiguous amount | Multiple submitted amounts remain ambiguous; application does not guess |
| request-04 | Wellness benefit | Benefit and applicable policy evidence are reported |
| request-05 | Prompt injection | Document text cannot change caller identity or switch tenant |
| request-06 | Exact duplicate | Separate result is retained and linked with `duplicate_of` |
| request-07 | Missing approval/amount | Missing information remains visible for human review |
| request-08 | Empty document | Item fails without preventing independent items from processing |

## Reproduce the Supplied Batch from the Command Line

PowerShell example:

```powershell
$files = Get-ChildItem .\sample-data\requests\request-*.txt, .\sample-data\requests\request-02.pdf
curl.exe -X POST http://localhost:8080/batches `
  -H "X-Caller-Id: atlas-employee-01" `
  -F "metadata=<sample-data/batch-metadata.json;type=application/json" `
  $(($files | ForEach-Object { "-F `"files=@$($_.FullName)`"" }) -join ' ')
```

If shell quoting makes the multipart command inconvenient, use Swagger UI, Postman, or another multipart
client with the same field names.

## Diagnostics and Traceability

Responses carry `batch_id` and `document_id`. Default processing does not log full submitted documents or
secrets. The current implementation does not persist a server-side audit store; traceability is
response-based for this local exercise.

## Known Test Gap

The automated tests do not exercise a live Spring-to-Python multipart HTTP call; the Python batch path and
Spring context are tested separately. This is deliberate because the local assessment environment does
not require a deployed integration stack.

## Future Enhancements

The assessment implementation intentionally uses deterministic offline retrieval and a model double.
For a production implementation, the provider can be replaced with Azure OpenAI behind a provider
abstraction without changing the public API contract.

For a larger policy corpus, policy passages can be chunked and embedded, with embeddings stored in
PostgreSQL using pgvector. Retrieval should first apply tenant, role, approval-status, and effective-date
filtering and then perform semantic similarity search. Only eligible evidence should be supplied to the
model.

A production implementation would also introduce formal authentication (OAuth2/OIDC), durable document
storage, audit persistence, monitoring, and, if processing volume or downstream approval latency requires
it, asynchronous batch processing through a queue.

## Production Evolution

Current assessment path:

```text
Spring Boot
    -> Python/FastAPI
    -> policy JSON
    -> deterministic retrieval
    -> offline model double
```

Possible production path:

```text
Spring Boot
    -> Python AI service
    -> caller/tenant/role/date eligibility filter
    -> embeddings
    -> PostgreSQL + pgvector
    -> eligible policy chunks
    -> Azure OpenAI
    -> answer + citations
```

The key security boundary remains unchanged: filter eligible policy evidence before retrieval results are
sent to a model. Submitted request text must never be treated as policy authority.
