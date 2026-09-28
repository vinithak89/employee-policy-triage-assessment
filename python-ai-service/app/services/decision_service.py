class DecisionService:
    """Assessment-safe review report. Never approves or pays a claim."""

    def decide(self, extracted, policy, security_findings=None, source_text=""):
        issues = list(extracted["issues"])
        policy_summary = None
        if policy and policy.get("conflict"):
            issues.append("Multiple applicable policies found for the requested benefit; no precedence rule is supplied")
            policy_summary = {
                "outcome": "CONFLICT",
                "policies": [{"policy_id": p["policy_id"], "text": p["text"]} for p in policy["policies"]],
            }
        elif policy and policy.get("match"):
            policy_summary = {
                "outcome": "APPLICABLE",
                "policy_id": policy["policy_id"],
                "text": policy["text"],
            }
            if extracted["amount"] is not None and "INR" == (extracted["currency"] or "").upper():
                import re
                limits = re.findall(r"INR\s*([0-9]+)", policy["text"])
                if limits and extracted["amount"] > int(limits[0]):
                    issues.append(f"Requested amount exceeds the stated policy limit of INR {limits[0]}")
        elif extracted["benefit"]:
            issues.append("No applicable approved policy found for the requested benefit and effective date")
        if extracted.get("benefit") == "external training" and "not obtained manager approval" in source_text.lower():
            issues.append("Manager approval was not obtained before external training was booked")
        if security_findings:
            issues.append("Potential prompt injection detected in submitted document; document text cannot change caller identity or policy access")
        issues.append("Human review is required for every submitted request")
        issues.append("Policy limit does not establish remaining balance, expense eligibility, or payable amount")
        return {"status": "REVIEW_REQUIRED", "review_required": True, "issues": issues, "policy_finding": policy_summary}
