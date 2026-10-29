#!/usr/bin/env python3
"""Validate Noor's canonical packs and generated lesson-level download assets."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "app/src/main/assets/form3_catalog.json"
LESSON_CATALOG_PATH = ROOT / "app/src/main/assets/lesson_catalog.json"
PACK_DIRECTORY = ROOT / "content/packs"
UNIT_DIRECTORY = ROOT / "content/lesson_units"
REPOSITORY = "https://raw.githubusercontent.com/1pagemw/noor-android"
BRANCHES = ("feature/android-scaffold", "main")
UNIT_SCHEMA_VERSION = 1


def fail(message: str) -> None:
    raise SystemExit(f"Content validation failed: {message}")


def load_json(path: Path, description: str) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(f"cannot read {description} {path}: {error}")


def unit_source_urls(pack_id: str, lesson_id: str) -> list[str]:
    suffix = f"content/lesson_units/{pack_id}/{lesson_id}.json"
    return [f"{REPOSITORY}/{branch}/{suffix}" for branch in BRANCHES]


def validate_pack(entry: dict) -> tuple[dict, int, int]:
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
            fail(f"duplicate topic id {topic['id']} in {pack_id}")
        topic_ids.add(topic["id"])
        for lesson in topic["lessons"]:
            if not all(lesson.get(key) for key in ("id", "title", "sections", "keyTerms", "questions")):
                fail(f"incomplete lesson in {topic['id']}")
            if lesson["id"] in lesson_ids:
                fail(f"duplicate lesson id {lesson['id']} in {pack_id}")
            lesson_ids.add(lesson["id"])
            for section in lesson["sections"]:
                if not section.get("heading") or not section.get("body"):
                    fail(f"empty lesson section in {lesson['id']}")
            for question in lesson["questions"]:
                if not all(question.get(key) for key in ("id", "prompt", "options", "explanation")):
                    fail(f"incomplete question in {lesson['id']}")
                if question["id"] in question_ids:
                    fail(f"duplicate question id {question['id']} in {pack_id}")
                question_ids.add(question["id"])
                if len(question["options"]) < 2:
                    fail(f"question {question['id']} has fewer than two options")
                if not 0 <= question["correctOptionIndex"] < len(question["options"]):
                    fail(f"invalid answer index in question {question['id']}")
                question_count += 1

    if not lesson_ids or not question_ids:
        fail(f"{pack_id} needs lessons and questions")
    return pack, len(topic_ids), question_count


def validate_lesson_units(packs: list[dict]) -> int:
    expected_paths: set[Path] = set()
    global_topic_ids: set[str] = set()
    global_lesson_ids: set[str] = set()
    global_question_ids: set[str] = set()
    expected_subjects: list[dict[str, Any]] = []
    unit_count = 0

    for pack in packs:
        pack_id = pack["id"]
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
            topic_id = topic["id"]
            if topic_id in global_topic_ids:
                fail(f"duplicate topic ID across packs: {topic_id}")
            global_topic_ids.add(topic_id)
            topic_entry: dict[str, Any] = {
                "topicId": topic_id,
                "title": topic["title"],
                "description": topic["description"],
                "lessons": [],
            }

            for lesson in topic["lessons"]:
                lesson_id = lesson["id"]
                if lesson_id in global_lesson_ids:
                    fail(f"duplicate lesson ID across packs: {lesson_id}")
                global_lesson_ids.add(lesson_id)
                for question in lesson["questions"]:
                    question_id = question["id"]
                    if question_id in global_question_ids:
                        fail(f"duplicate question ID across packs: {question_id}")
                    global_question_ids.add(question_id)

                relative_path = Path(pack_id) / f"{lesson_id}.json"
                unit_path = UNIT_DIRECTORY / relative_path
                expected_paths.add(unit_path)
                if not unit_path.is_file():
                    fail(f"missing generated lesson unit {relative_path.as_posix()}")
                unit_bytes = unit_path.read_bytes()
                digest = hashlib.sha256(unit_bytes).hexdigest()
                try:
                    payload = json.loads(unit_bytes)
                except json.JSONDecodeError as error:
                    fail(f"invalid JSON in {unit_path}: {error}")

                expected_payload = {
                    "schemaVersion": UNIT_SCHEMA_VERSION,
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
                if payload != expected_payload:
                    fail(f"lesson unit does not exactly match canonical source: {relative_path.as_posix()}")
                if set(payload) != set(expected_payload):
                    fail(f"unexpected or missing payload fields in {relative_path.as_posix()}")

                topic_entry["lessons"].append(
                    {
                        "lessonId": lesson_id,
                        "title": lesson["title"],
                        "readTimeMinutes": lesson.get("readTimeMinutes"),
                        "sizeBytes": len(unit_bytes),
                        "sha256": digest,
                        "sourceUrls": unit_source_urls(pack_id, lesson_id),
                    }
                )
                unit_count += 1
            subject_entry["topics"].append(topic_entry)
        expected_subjects.append(subject_entry)

    actual_paths = {path for path in UNIT_DIRECTORY.rglob("*") if path.is_file()} if UNIT_DIRECTORY.exists() else set()
    if actual_paths != expected_paths:
        stale = sorted(path.relative_to(UNIT_DIRECTORY).as_posix() for path in actual_paths - expected_paths)
        missing = sorted(path.relative_to(UNIT_DIRECTORY).as_posix() for path in expected_paths - actual_paths)
        fail(f"generated lesson file mismatch: stale={stale}, missing={missing}")

    lesson_catalog = load_json(LESSON_CATALOG_PATH, "lesson catalogue")
    if not isinstance(lesson_catalog, dict) or lesson_catalog.get("catalogVersion") != 1:
        fail("lesson catalogue must have catalogVersion 1")
    if lesson_catalog.get("subjects") != expected_subjects:
        fail("lesson catalogue metadata/order/size/hash/URLs do not match generated units and canonical packs")

    print(f"OK lesson assets: {unit_count} units, sizes/SHA-256/URLs verified, index contains metadata only")
    return unit_count


def main() -> None:
    catalog = load_json(CATALOG_PATH, "subject catalogue")
    entries = catalog.get("packs", [])
    if not entries:
        fail("catalogue has no packs")
    if len({entry.get("id") for entry in entries}) != len(entries):
        fail("catalogue pack IDs must be unique")

    packs: list[dict] = []
    for entry in entries:
        pack, topic_count, question_count = validate_pack(entry)
        packs.append(pack)
        print(f"OK {entry['id']}: {topic_count} topics, {question_count} questions, SHA-256 verified")

    catalogue_ids = {entry["id"] for entry in entries}
    file_ids = {path.stem for path in PACK_DIRECTORY.glob("form3_*.json")}
    if catalogue_ids != file_ids:
        fail(f"catalogue/file mismatch: catalogue={sorted(catalogue_ids)}, files={sorted(file_ids)}")

    unit_count = validate_lesson_units(packs)
    print(f"Validated {len(entries)} Form 3 lesson packs and {unit_count} lesson units.")


if __name__ == "__main__":
    main()
