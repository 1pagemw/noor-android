#!/usr/bin/env python3
"""Validate Noor's offline content packs and their in-app download catalogue."""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "app/src/main/assets/form3_catalog.json"
PACK_DIRECTORY = ROOT / "content/packs"


def fail(message: str) -> None:
    raise SystemExit(f"Content validation failed: {message}")


def validate_pack(entry: dict) -> tuple[int, int]:
    pack_id = entry.get("id")
    pack_path = PACK_DIRECTORY / f"{pack_id}.json"
    if not pack_path.is_file():
        fail(f"missing pack file for {pack_id}")

    raw = pack_path.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    if digest != entry.get("sha256", "").lower():
        fail(f"SHA-256 mismatch for {pack_id}: expected {entry.get('sha256')}, got {digest}")

    try:
        pack = json.loads(raw)
    except json.JSONDecodeError as error:
        fail(f"invalid JSON in {pack_path}: {error}")

    for key in ("id", "form", "subject", "language", "contentVersion", "curriculumStatus", "topics"):
        if key not in pack:
            fail(f"{pack_id} is missing {key}")
    if pack["id"] != pack_id or pack["form"] != entry.get("form") or pack["subject"] != entry.get("subject"):
        fail(f"catalogue metadata disagrees with {pack_id}")
    if pack["contentVersion"] != entry.get("contentVersion"):
        fail(f"content version disagrees with catalogue for {pack_id}")
    if len(pack["topics"]) != entry.get("topicCount"):
        fail(f"topic count disagrees with catalogue for {pack_id}")
    if not pack["topics"]:
        fail(f"{pack_id} has no topics")
    if "Draft" not in pack["curriculumStatus"]:
        fail(f"{pack_id} must retain its draft-review status")

    topic_ids: set[str] = set()
    lesson_ids: set[str] = set()
    question_ids: set[str] = set()
    question_count = 0
    for topic in pack["topics"]:
        if not all(topic.get(key) for key in ("id", "title", "description", "lessons")):
            fail(f"incomplete topic in {pack_id}")
        if topic["id"] in topic_ids:
            fail(f"duplicate topic id {topic['id']}")
        topic_ids.add(topic["id"])
        for lesson in topic["lessons"]:
            if not all(lesson.get(key) for key in ("id", "title", "sections", "keyTerms", "questions")):
                fail(f"incomplete lesson in {topic['id']}")
            if lesson["id"] in lesson_ids:
                fail(f"duplicate lesson id {lesson['id']}")
            lesson_ids.add(lesson["id"])
            for section in lesson["sections"]:
                if not section.get("heading") or not section.get("body"):
                    fail(f"empty lesson section in {lesson['id']}")
            for question in lesson["questions"]:
                if not all(question.get(key) for key in ("id", "prompt", "options", "explanation")):
                    fail(f"incomplete question in {lesson['id']}")
                if question["id"] in question_ids:
                    fail(f"duplicate question id {question['id']}")
                question_ids.add(question["id"])
                if len(question["options"]) < 2:
                    fail(f"question {question['id']} has fewer than two options")
                if not 0 <= question["correctOptionIndex"] < len(question["options"]):
                    fail(f"invalid answer index in question {question['id']}")
                question_count += 1

    if not lesson_ids or not question_ids:
        fail(f"{pack_id} needs lessons and questions")
    return len(topic_ids), question_count


def main() -> None:
    try:
        catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(f"cannot read catalogue: {error}")

    entries = catalog.get("packs", [])
    if not entries:
        fail("catalogue has no packs")
    if len({entry.get("id") for entry in entries}) != len(entries):
        fail("catalogue pack IDs must be unique")

    for entry in entries:
        topic_count, question_count = validate_pack(entry)
        print(f"OK {entry['id']}: {topic_count} topics, {question_count} questions, SHA-256 verified")

    catalogue_ids = {entry["id"] for entry in entries}
    file_ids = {path.stem for path in PACK_DIRECTORY.glob("form3_*.json")}
    if catalogue_ids != file_ids:
        fail(f"catalogue/file mismatch: catalogue={sorted(catalogue_ids)}, files={sorted(file_ids)}")

    print(f"Validated {len(entries)} Form 3 lesson packs.")


if __name__ == "__main__":
    main()
