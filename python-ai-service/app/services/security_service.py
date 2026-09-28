import re


class SecurityService:
    PROMPT_INJECTION_PATTERNS = [
        r"ignore\s+(all\s+)?(?:previous|prior)\s+(?:instructions|rules)",
        r"system\s+message", r"system\s+prompt", r"developer\s+instructions",
        r"switch\s+the\s+caller", r"mark\s+this\s+request\s+approved", r"override\s+instructions",
        r"bypass\s+security", r"reveal\s+the\s+prompt", r"show\s+me\s+the\s+prompt",
    ]

    def detect_prompt_injection(self, text: str) -> list[str]:
        return [pattern for pattern in self.PROMPT_INJECTION_PATTERNS if re.search(pattern, text, re.IGNORECASE)]
