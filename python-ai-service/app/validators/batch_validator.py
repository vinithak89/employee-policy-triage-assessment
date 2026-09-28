from collections import Counter


def validate_manifest(documents):
    document_ids = [
        document.document_id
        for document in documents
    ]

    filenames = [
        document.filename
        for document in documents
    ]

    if len(document_ids) != len(set(document_ids)):
        return "Duplicate document_id in manifest"

    if len(filenames) != len(set(filenames)):
        return "Duplicate filename in manifest"

    return None


def validate_uploaded_files(documents, files):
    manifest_filenames = [
        document.filename
        for document in documents
    ]

    uploaded_filenames = [
        file.filename
        for file in files
    ]

    manifest_counts = Counter(manifest_filenames)
    uploaded_counts = Counter(uploaded_filenames)

    missing_files = [
        filename
        for filename in manifest_filenames
        if uploaded_counts[filename] == 0
    ]

    if missing_files:
        return {
            "message": "Missing file parts",
            "files": sorted(set(missing_files))
        }

    extra_files = [
        filename
        for filename in uploaded_filenames
        if filename not in manifest_counts
    ]

    if extra_files:
        return {
            "message": "Extra file parts",
            "files": sorted(set(extra_files))
        }

    duplicate_uploaded_files = [
        filename
        for filename, count in uploaded_counts.items()
        if count > 1
    ]

    if duplicate_uploaded_files:
        return {
            "message": "Duplicate file parts",
            "files": sorted(duplicate_uploaded_files)
        }

    if len(uploaded_filenames) != len(manifest_filenames):
        return {
            "message": "File count does not match manifest",
            "expected": len(manifest_filenames),
            "received": len(uploaded_filenames)
        }

    return None