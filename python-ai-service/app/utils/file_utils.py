from io import BytesIO
import hashlib

from pypdf import PdfReader


def calculate_file_hash(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def extract_text_from_txt(content: bytes) -> str:
    return content.decode("utf-8").strip()


def extract_text_from_pdf(content: bytes) -> str:
    reader = PdfReader(BytesIO(content))

    pages = []

    for page in reader.pages:
        text = page.extract_text()

        if text:
            pages.append(text)

    return "\n".join(pages).strip()


def extract_text(filename: str, content: bytes) -> str:
    filename_lower = filename.lower()

    if filename_lower.endswith(".txt"):
        return extract_text_from_txt(content)

    if filename_lower.endswith(".pdf"):
        return extract_text_from_pdf(content)

    raise ValueError(f"Unsupported file type: {filename}")