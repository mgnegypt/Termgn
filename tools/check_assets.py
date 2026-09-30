#!/usr/bin/env python3
"""CI gate: every shipped asset/font must have a LICENSES.md row.

Usage:
  python3 tools/check_assets.py [--apk path/to.apk] [--max-mb 150]

Fails (exit 1) when:
  - any file under app/src/main/assets/ or design font binaries lacks a row,
  - any LICENSES.md file-row no longer exists on disk,
  - the APK (if given) exceeds the size budget.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LICENSES = ROOT / "assets" / "LICENSES.md"
APK_BUDGET_MB = 150


def asset_files():
    out = []
    assets = ROOT / "app" / "src" / "main" / "assets"
    if assets.is_dir():
        out += [p for p in assets.rglob("*") if p.is_file()]
    fonts = ROOT / "design" / "src" / "main" / "res" / "font"
    if fonts.is_dir():
        out += [p for p in fonts.iterdir() if p.suffix.lower() == ".ttf"]
    return out


def license_rows():
    rows = set()
    for line in LICENSES.read_text(encoding="utf-8").splitlines():
        m = re.match(r"\|\s*`([^`]+)`\s*\|", line.strip())
        if m and m.group(1) != "Asset path":
            rows.add(m.group(1))
    return rows


def main() -> int:
    args = sys.argv[1:]
    apk = None
    max_mb = APK_BUDGET_MB
    if "--apk" in args:
        apk = Path(args[args.index("--apk") + 1])
    if "--max-mb" in args:
        max_mb = float(args[args.index("--max-mb") + 1])

    errors = []
    rows = license_rows()

    for f in asset_files():
        rel = f.relative_to(ROOT).as_posix()
        if rel not in rows:
            errors.append(f"asset without LICENSES.md row: {rel}")

    for row in sorted(rows):
        if row.startswith("*") or row.startswith("("):
            continue  # note rows, not file paths
        if "*" in row:
            if not list(ROOT.glob(row)):
                errors.append(f"LICENSES.md glob matches nothing: {row}")
        elif not (ROOT / row).exists():
            errors.append(f"LICENSES.md row missing on disk: {row}")

    if apk is not None:
        if not apk.exists():
            errors.append(f"APK not found: {apk}")
        else:
            mb = apk.stat().st_size / (1024 * 1024)
            print(f"APK size: {mb:.1f}MB (budget {max_mb:.0f}MB)")
            if mb > max_mb:
                errors.append(f"APK exceeds budget: {mb:.1f}MB > {max_mb:.0f}MB")

    if errors:
        print("ASSET GATE FAILURES:")
        for e in errors:
            print(f" - {e}")
        return 1
    print(f"asset gate OK ({len(rows)} license rows)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
