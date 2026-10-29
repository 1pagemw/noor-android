#!/usr/bin/env python3
"""Generate lesson-level JSON payloads and the lightweight lesson catalogue."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
PACK_DIRECTORY = ROOT / "content/packs"
UNIT_DIRECTORY = ROOT / "content/lesson_units"
INDEX_PATH = ROOT / "app/src/main/assets/lesson_catalog.json"
PACK_IDS = ("form3_biology", "form3_physics")
REPOSITORY = "https://raw.githubusercontent.com/1pagemw/noor-android"
BRANCHES = ("feature/android-scaffold", "main")
SCHEMA_VERSION = 1
CATALOG_VERSION = 1


def json_bytes(value: Any) -> bytes:
    """Serialize JSON consistently for reproducible hashes and diffs."""
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def source_urls(pack_id: str, lesson_id: str) -> list[str]:
    suffix = f"content/lesson_units/{pack_id}/{lesson_id}.json"
    return [f"{REPOSITORY}/{branch}/{suffix}" for branch in BRANCHES]


def safe_id(value: Any, description: str) -> str:
    if not isinstance(value, str) or not value or "/" in value or "\\" in value or value in {".", ".."}:
        raise ValueError(f"invalid {description}: {value!r}")
    return value


def main() -> None:
    expected_paths: set[Path] = set()
    subjects: list[dict[str, Any]] = []

    for pack_id in PACK_IDS:
        pack_path = PACK_DIRECTORY / f"{pack_id}.json"
        pack = json.loads(pack_path.read_text(encoding="utf-8"))
        if pack.get("id") != pack_id:
            raise ValueError(f"pack ID mismatch in {pack_path}")

        subject_entry: dict[str, Any] = {
            "packId": pack_id,
            "form": pack["form"],
            "subject": pack["subject"],
            "language": pack["language"],
            "contentVersion": pack["contentVersion"],
            "curriculumStatus": pack["curriculumStatus"],
            "topics": [],
        }

        for topic in pack["topics"]:
            topic_id = safe_id(topic.get("id"), "topic ID")
            topic_entry: dict[str, Any] = {
                "topicId": topic_id,
                "title": topic["title"],
                "description": topic["description"],
                "lessons": [],
            }

            for lesson in topic["lessons"]:
                lesson_id = safe_id(lesson.get("id"), "lesson ID")
                payload = {
                    "schemaVersion": SCHEMA_VERSION,
                    "packId": pack_id,
                    "form": pack["form"],
                    "subject": pack["subject"],
                    "language": pack["language"],
                    "contentVersion": pack["contentVersion"],
                    "curriculumStatus": pack["curriculumStatus"],
                    "topicId": topic_id,
                    "topicTitle": topic["title"],
                    "topicDescription": topic["description"],
                    "lesson": lesson,
                }
                payload_bytes = json_bytes(payload)
                relative_path = Path(pack_id) / f"{lesson_id}.json"
                target_path = UNIT_DIRECTORY / relative_path
                expected_paths.add(target_path)
                target_path.parent.mkdir(parents=True, exist_ok=True)
                target_path.write_bytes(payload_bytes)

                topic_entry["lessons"].append(
                    {
                        "lessonId": lesson_id,
                        "title": lesson["title"],
                        "readTimeMinutes": lesson.get("readTimeMinutes"),
                        "sizeBytes": len(payload_bytes),
                        "sha256": hashlib.sha256(payload_bytes).hexdigest(),
                        "sourceUrls": source_urls(pack_id, lesson_id),
                    }
                )
            subject_entry["topics"].append(topic_entry)
        subjects.append(subject_entry)

    # Remove obsolete outputs, but only from the generated lesson-unit tree.
    if UNIT_DIRECTORY.exists():
        for path in sorted(UNIT_DIRECTORY.rglob("*"), reverse=True):
            if path.is_file() and path not in expected_paths:
                path.unlink()
            elif path.is_dir() and not any(path.iterdir()):
                path.rmdir()
    UNIT_DIRECTORY.mkdir(parents=True, exist_ok=True)

    index = {"catalogVersion": CATALOG_VERSION, "subjects": subjects}
    INDEX_PATH.parent.mkdir(parents=True, exist_ok=True)
    INDEX_PATH.write_bytes(json_bytes(index))
    print(f"Generated {len(expected_paths)} lesson units and {INDEX_PATH.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
