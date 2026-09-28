import re


class FieldExtractionService:
    REFERENCE_PATTERN = re.compile(r"\bReference\s*:\s*([A-Za-z0-9-]+)", re.IGNORECASE)
    CURRENCY_AMOUNT_PATTERN = re.compile(r"\b(INR)\s*([0-9]+(?:\.[0-9]+)?)\b|\b([0-9]+(?:\.[0-9]+)?)\s*(INR)\b|₹\s*([0-9]+(?:\.[0-9]+)?)", re.IGNORECASE)

    BENEFIT_RULES = [
        ("certification reimbursement", ["certification reimbursement"]),
        ("home-office allowance", ["home-office allowance", "home office allowance"]),
        ("wellness benefit", ["wellness benefit", "gym membership"]),
        ("external training", ["external training", "training reimbursement"]),
        ("rail travel", ["rail travel", "train travel"]),
    ]

    def extract_fields(self, text: str) -> dict:
        benefit, benefit_evidence = self._extract_benefit(text)
        reference, reference_evidence = self._extract_reference(text)
        amounts, currencies, amount_evidence = self._extract_amounts(text)
        issues = []
        amount = amounts[0] if len(amounts) == 1 else None
        currency = currencies[0] if currencies and len(set(currencies)) == 1 else (currencies[0] if currencies else None)
        if len(amounts) > 1:
            issues.append("Amount is ambiguous because multiple monetary amounts were found in the document")
        if len(set(currencies)) > 1:
            issues.append("Currency is ambiguous because multiple currencies were found in the document")
            currency = None
        if benefit is None:
            issues.append("Requested benefit could not be identified")
        if reference is None:
            issues.append("Reference could not be identified")
        if not amounts:
            issues.append("Amount is not available in the document")
        evidence = {}
        if benefit_evidence:
            evidence["benefit"] = benefit_evidence
        if amount_evidence:
            evidence["amount"] = amount_evidence[0] if len(amount_evidence) == 1 else amount_evidence
            evidence["currency"] = amount_evidence[0] if len(amount_evidence) == 1 else amount_evidence
        if reference_evidence:
            evidence["reference"] = reference_evidence
        return {"benefit": benefit, "amount": amount, "currency": currency, "reference": reference, "field_evidence": evidence, "issues": issues}

    def _extract_benefit(self, text):
        for line in self._get_lines(text):
            low = line.lower()
            for benefit, keywords in self.BENEFIT_RULES:
                if any(k in low for k in keywords):
                    return benefit, line.strip()
        return None, None

    def _extract_reference(self, text):
        m = self.REFERENCE_PATTERN.search(text)
        return (m.group(1), m.group(0).strip()) if m else (None, None)

    def _extract_amounts(self, text):
        amounts, currencies, evidence = [], [], []
        for m in self.CURRENCY_AMOUNT_PATTERN.finditer(text):
            value = next((g for g in (m.group(2), m.group(3), m.group(5)) if g is not None), None)
            curr = m.group(1) or m.group(4) or ("INR" if m.group(5) else None)
            if value is None or curr is None:
                continue
            amounts.append(float(value) if "." in value else int(value))
            currencies.append(curr.upper())
            evidence.append(self._get_containing_line(text, m.start()))
        return amounts, currencies, evidence

    @staticmethod
    def _get_lines(text):
        return [line.strip() for line in text.splitlines() if line.strip()]

    @staticmethod
    def _get_containing_line(text, position):
        start = text.rfind("\n", 0, position)
        start = 0 if start == -1 else start + 1
        end = text.find("\n", position)
        return text[start:] if end == -1 else text[start:end].strip()
