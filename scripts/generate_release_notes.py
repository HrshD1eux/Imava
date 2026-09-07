#!/usr/bin/env python3
"""
Automated Release Notes Generator for Imava.
- Extracts matching version section from RELEASE_NOTES.md if present.
- Otherwise, automatically generates categorized release notes from git commit history
  since the last tag.
- Can update RELEASE_NOTES.md archive automatically.
"""

import argparse
import os
import re
import subprocess
import sys

if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass


def extract_version_from_gradle():
    gradle_path = os.path.join("app", "build.gradle.kts")
    if not os.path.exists(gradle_path):
        return "1.0.0"
    with open(gradle_path, "r", encoding="utf-8") as f:
        content = f.read()
    match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    return match.group(1) if match else "1.0.0"


def get_section_from_file(file_path, version):
    if not os.path.exists(file_path):
        return None

    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Pattern matches "## ... v1.1.30" or "## ... 1.1.30" down to the next "## " or EOF
    ver_pattern = re.escape(version)
    pattern = rf"(##\s+.*?(?:v)?{ver_pattern}\b.*?)(?=\n##\s+|\Z)"
    match = re.search(pattern, content, re.DOTALL | re.IGNORECASE)
    if match:
        return match.group(1).strip()
    return None


def get_previous_git_tag():
    try:
        result = subprocess.run(
            ["git", "describe", "--tags", "--abbrev=0", "HEAD~1"],
            capture_output=True,
            text=True,
            check=False
        )
        tag = result.stdout.strip()
        if tag:
            return tag
    except Exception:
        pass

    try:
        result = subprocess.run(
            ["git", "describe", "--tags", "--abbrev=0"],
            capture_output=True,
            text=True,
            check=False
        )
        tag = result.stdout.strip()
        if tag:
            return tag
    except Exception:
        pass

    return None


def generate_notes_from_git(version):
    prev_tag = get_previous_git_tag()
    git_range = f"{prev_tag}..HEAD" if prev_tag else "HEAD~10..HEAD"

    try:
        result = subprocess.run(
            ["git", "log", git_range, "--pretty=format:%s%x00%b%x1e"],
            capture_output=True,
            text=True,
            check=False
        )
        raw_entries = result.stdout.split("\x1e")
    except Exception:
        raw_entries = []

    features = []
    fixes = []
    improvements = []
    others = []

    for entry in raw_entries:
        if not entry.strip():
            continue
        parts = entry.strip().split("\x00")
        subject = parts[0].strip() if parts else ""
        body = parts[1].strip() if len(parts) > 1 else ""

        # Ignore automated commits
        lower = subject.lower()
        if any(skip in lower for skip in ["[skip ci]", "bump version", "merge branch", "sync screenshots"]):
            continue

        # Extract bullet points from body if body contains detailed bullets
        sub_bullets = []
        if body:
            for line in body.splitlines():
                line = line.strip()
                if line.startswith("- ") or line.startswith("* "):
                    bullet_text = line[2:].strip()
                    if bullet_text and not any(skip in bullet_text.lower() for skip in ["[skip ci]", "bump version"]):
                        sub_bullets.append(bullet_text)

        # Categorize
        clean_subject = re.sub(
            r"^(feat|fix|perf|refactor|docs|chore|style)(\([^)]+\))?:\s*",
            "",
            subject,
            flags=re.IGNORECASE
        ).strip()
        if clean_subject:
            clean_subject = clean_subject[0].upper() + clean_subject[1:]

        category = "other"
        if re.match(r"^feat(\([^)]+\))?:", subject, re.IGNORECASE):
            category = "feat"
        elif re.match(r"^fix(\([^)]+\))?:", subject, re.IGNORECASE):
            category = "fix"
        elif re.match(r"^(perf|refactor|style)(\([^)]+\))?:", subject, re.IGNORECASE):
            category = "perf"

        formatted_items = []
        if sub_bullets:
            for b in sub_bullets:
                formatted_items.append(f"* {b}")
        elif clean_subject:
            formatted_items.append(f"* {clean_subject}")

        if category == "feat":
            features.extend(formatted_items)
        elif category == "fix":
            fixes.extend(formatted_items)
        elif category == "perf":
            improvements.extend(formatted_items)
        else:
            others.extend(formatted_items)

    sections = [f"## 🚀 What's New in Imava v{version}\n"]

    if features:
        sections.append("### ✨ New Features & Enhancements\n" + "\n".join(dict.fromkeys(features)))
    if fixes:
        sections.append("### 🐛 Bug Fixes & Stability\n" + "\n".join(dict.fromkeys(fixes)))
    if improvements:
        sections.append("### ⚡ Performance & Polish\n" + "\n".join(dict.fromkeys(improvements)))
    if not features and not fixes and not improvements and others:
        sections.append("### 🛠️ Changes\n" + "\n".join(dict.fromkeys(others)))

    sections.append("---\n\n### 📦 Downloads & Verification\n* Download the signed APK below.\n* Zero cloud dependencies, 100% offline, zero analytics.")

    return "\n\n".join(sections).strip()


def main():
    parser = argparse.ArgumentParser(description="Generate release notes for Imava.")
    parser.add_argument("--version", help="Version name (defaults to app/build.gradle.kts)")
    parser.add_argument("--output", default="release_body.md", help="Output markdown file path")
    parser.add_argument("--notes-file", default="RELEASE_NOTES.md", help="Path to RELEASE_NOTES.md")
    parser.add_argument("--print", action="store_true", help="Print output to stdout")
    args = parser.parse_args()

    version = args.version or extract_version_from_gradle()
    version = version.lstrip("v")

    notes = get_section_from_file(args.notes_file, version)
    if notes:
        print(f"[+] Found pre-written release notes for v{version} in {args.notes_file}")
    else:
        print(f"[!] No release notes found for v{version} in {args.notes_file}. Generating from git commit history...")
        notes = generate_notes_from_git(version)

    with open(args.output, "w", encoding="utf-8") as f:
        f.write(notes)
    print(f"[SUCCESS] Wrote release notes to {args.output}")

    if getattr(args, "print", False):
        print("\n--- RELEASE NOTES ---")
        print(notes)
        print("---------------------\n")


if __name__ == "__main__":
    main()
