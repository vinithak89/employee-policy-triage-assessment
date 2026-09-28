# Implementation Decision Note

1. **Offline deterministic provider.** The assessment explicitly permits simple retrieval and requires a deterministic offline model double. The implementation therefore avoids paid Azure OpenAI/API keys while keeping a provider boundary that can be replaced by a real model later.
2. **Metadata-first policy eligibility.** Tenant, role, approval state, and effective interval are applied before retrieval/generation. Submitted request text cannot claim a different tenant or role.
3. **Policy corpus in JSON.** The twelve supplied records are kept in one data file, separate from service logic. Exact duplicate records by policy ID do not change behavior; distinct simultaneously applicable records remain a conflict.
4. **No claim approval.** The batch service always returns `review_required=true`. Limits are reported as evidence/finding only; no remaining balance, eligibility, or payable amount is inferred.
5. **Deterministic extraction.** TXT/PDF extraction uses explicit currency/amount patterns and source-line evidence. Multiple amounts remain ambiguous instead of selecting one.
6. **Security isolation.** Prompt-injection detection is a review finding. It never changes caller context or policy eligibility, and ineligible passages are excluded before provider generation.
7. **Technical failures remain technical.** Provider timeout/unavailable/malformed output use explicit exception types and are not represented as insufficient evidence.
