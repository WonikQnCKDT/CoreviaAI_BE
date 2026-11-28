#!/usr/bin/env python3
"""
FE docs/i18n/label-keys.md에서 다국어 라벨 키를 읽어:
  1) Flyway 시드 SQL(V29__seed_fe_labels.sql)을 생성(--generate, 기본 동작)
  2) FE 라벨 키가 BE Flyway 마이그레이션 SQL에 모두 존재하는지 검증(--verify)

사용 예:
  python scripts/generate_i18n_seed.py --generate
  python scripts/generate_i18n_seed.py --verify
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

BE_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_FE_DOC = BE_ROOT.parent / "CoreviaAI_FE" / "docs" / "i18n" / "label-keys.md"
DEFAULT_SNAPSHOT = BE_ROOT / "src" / "test" / "integration" / "resources" / "i18n" / "label-keys.md"
DEFAULT_OUTPUT = BE_ROOT / "src" / "main" / "resources" / "db" / "migration" / "V29__seed_fe_labels.sql"
MIGRATION_DIR = BE_ROOT / "src" / "main" / "resources" / "db" / "migration"

LABEL_KEY_RE = re.compile(r"^(?=.{1,100}$)[A-Za-z][A-Za-z0-9_-]*(\.[A-Za-z0-9_-]+)*$")
ROW_RE = re.compile(r"^\|\s*`([^`]+)`\s*\|\s*(.*?)\s*\|\s*([^|]*?)\s*\|$")
SQL_SEED_KEY_RE = re.compile(
    r"\(\s*'([A-Za-z][A-Za-z0-9_.-]*)'\s*,\s*'(?:''|[^'])*'\s*,"
)


def unescape_markdown_cell(text: str) -> str:
    """FE catalog.test.ts의 escapeCell(|, *) 역변환."""
    return re.sub(r"\\([|*])", r"\1", text).strip()


def escape_sql_literal(text: str) -> str:
    """PostgreSQL/H2 문자열 리터럴 작은따옴표 이스케이프."""
    return text.replace("'", "''")


def parse_label_doc(doc_path: Path) -> tuple[list[tuple[str, str]], list[tuple[str, str]]]:
    """
    label-keys.md를 파싱해 (등록 필요 목록, 이미 시드됨 목록)을 반환한다.
    각 항목은 (label_key, text_ko) 튜플이다.
    """
    content = doc_path.read_text(encoding="utf-8")
    section: str | None = None
    to_register: list[tuple[str, str]] = []
    already_seeded: list[tuple[str, str]] = []

    for line_no, raw_line in enumerate(content.splitlines(), start=1):
        line = raw_line.strip()
        if line.startswith("## 등록 필요"):
            section = "register"
            continue
        if line.startswith("## BE에 이미 시드됨"):
            section = "seeded"
            continue
        if line.startswith("## "):
            section = None
            continue
        if section is None or not line.startswith("| `"):
            continue

        match = ROW_RE.match(line)
        if not match:
            raise ValueError(f"{doc_path}:{line_no} 표 행을 파싱할 수 없습니다: {line}")

        key = match.group(1).strip()
        text_ko = unescape_markdown_cell(match.group(2))
        if not LABEL_KEY_RE.match(key):
            raise ValueError(f"{doc_path}:{line_no} 잘못된 라벨 키 형식: {key}")
        if not text_ko or len(text_ko) > 500:
            raise ValueError(f"{doc_path}:{line_no} 잘못된 한국어 문구 길이({len(text_ko)}): {key}")

        if section == "register":
            to_register.append((key, text_ko))
        else:
            already_seeded.append((key, text_ko))

    return to_register, already_seeded


def extract_seeded_keys_from_migrations(migration_dir: Path) -> set[str]:
    """V*.sql 마이그레이션 파일에서 i18n_labels에 INSERT되는 모든 label_key를 추출한다."""
    keys: set[str] = set()
    for sql_file in sorted(migration_dir.glob("V*.sql")):
        text = sql_file.read_text(encoding="utf-8")
        if "i18n_labels" not in text:
            continue
        # INSERT INTO i18n_labels ... ; 블록만 추출
        for block in re.findall(
            r"INSERT\s+INTO\s+i18n_labels\b.*?;",
            text,
            flags=re.IGNORECASE | re.DOTALL,
        ):
            for match in SQL_SEED_KEY_RE.finditer(block):
                keys.add(match.group(1))
    return keys


def render_migration_sql(entries: list[tuple[str, str]]) -> str:
    header = [
        "-- FR-ADM-06 FE 다국어 라벨 시드 (SCRUM-169)",
        "--   원본: CoreviaAI_FE/docs/i18n/label-keys.md (자동 생성: python scripts/generate_i18n_seed.py)",
        "--   text_ko는 FE 기본 한국어 문구를 그대로 넣고, text_en·text_zh_tw는 NULL로 비워 둔다(비우면 한국어로 표시).",
        "--   기존 시드(V9 공통 코드, V24 메뉴)나 관리자가 먼저 등록한 라벨과 겹치는 키는 ON CONFLICT DO NOTHING으로 건너뛴다.",
        "-- 기존 데이터 영향: 누락된 FE 라벨 행만 추가한다. 기존 행은 수정하지 않는다.",
        "-- 되돌릴 때는 이 마이그레이션으로 추가한 라벨을 정리하는 새 버전을 추가한다(roll-forward).",
        "",
        "INSERT INTO i18n_labels (label_key, text_ko, text_en, text_zh_tw, created_at, updated_at) VALUES",
    ]
    rows: list[str] = []
    for idx, (key, text_ko) in enumerate(entries):
        suffix = "," if idx < len(entries) - 1 else ""
        escaped_key = escape_sql_literal(key)
        escaped_ko = escape_sql_literal(text_ko)
        rows.append(
            f"    ('{escaped_key}', '{escaped_ko}', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP){suffix}"
        )
    footer = [
        "ON CONFLICT DO NOTHING;",
        "",
    ]
    return "\n".join(header + rows + footer)


def resolve_doc_path(explicit: Path | None) -> Path:
    if explicit is not None:
        if not explicit.is_file():
            raise FileNotFoundError(f"라벨 문서를 찾을 수 없습니다: {explicit}")
        return explicit
    if DEFAULT_FE_DOC.is_file():
        return DEFAULT_FE_DOC
    if DEFAULT_SNAPSHOT.is_file():
        return DEFAULT_SNAPSHOT
    raise FileNotFoundError(
        f"FE 라벨 문서를 찾을 수 없습니다. 확인한 경로: {DEFAULT_FE_DOC}, {DEFAULT_SNAPSHOT}"
    )


def cmd_generate(doc_path: Path, output_path: Path, snapshot_path: Path | None) -> int:
    to_register, already_seeded = parse_label_doc(doc_path)
    if not to_register:
        print("오류: '등록 필요' 섹션에서 라벨을 찾지 못했습니다.", file=sys.stderr)
        return 1

    sql = render_migration_sql(to_register)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(sql, encoding="utf-8", newline="\n")

    if snapshot_path is not None and doc_path.resolve() != snapshot_path.resolve():
        snapshot_path.parent.mkdir(parents=True, exist_ok=True)
        snapshot_path.write_text(doc_path.read_text(encoding="utf-8"), encoding="utf-8", newline="\n")

    print(
        f"생성 완료: {output_path} (등록 필요 {len(to_register)}개, 기존 시드 {len(already_seeded)}개)"
    )
    return 0


def cmd_verify(doc_path: Path, migration_dir: Path) -> int:
    to_register, already_seeded = parse_label_doc(doc_path)
    fe_keys = {key for key, _ in to_register} | {key for key, _ in already_seeded}
    if not fe_keys:
        print("오류: FE 라벨 문서에서 키를 찾지 못했습니다.", file=sys.stderr)
        return 1

    db_keys = extract_seeded_keys_from_migrations(migration_dir)
    missing = sorted(fe_keys - db_keys)
    if missing:
        print(
            f"실패: FE 라벨 키 {len(missing)}개가 BE 마이그레이션(i18n_labels)에 누락되었습니다:",
            file=sys.stderr,
        )
        for key in missing[:30]:
            print(f"  - {key}", file=sys.stderr)
        if len(missing) > 30:
            print(f"  ... 외 {len(missing) - 30}개", file=sys.stderr)
        return 1

    print(
        f"검증 통과: FE 라벨 키 {len(fe_keys)}개가 모두 BE 마이그레이션({len(db_keys)}개 시드)에 존재합니다. (기준: {doc_path})"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="FE 다국어 라벨 Flyway 시드 생성 및 검증")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--generate", action="store_true", help="V29 시드 SQL 생성 (기본값)")
    mode.add_argument("--verify", action="store_true", help="FE 라벨 키가 모두 BE 마이그레이션에 있는지 검증")
    parser.add_argument("--input", type=Path, default=None, help="FE label-keys.md 경로")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="생성할 마이그레이션 SQL 경로")
    parser.add_argument("--no-snapshot", action="store_true", help="통합 테스트용 스냅샷 복사 생략")
    args = parser.parse_args()

    doc_path = resolve_doc_path(args.input)
    if args.verify:
        return cmd_verify(doc_path, MIGRATION_DIR)
    snapshot = None if args.no_snapshot else DEFAULT_SNAPSHOT
    return cmd_generate(doc_path, args.output, snapshot)


if __name__ == "__main__":
    sys.exit(main())
