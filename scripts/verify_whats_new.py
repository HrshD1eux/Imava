#!/usr/bin/env python3
"""
Automated What's New & Release Notes Verifier for Imava.
Enforces GEMINI.md Rule 6 before every commit:
1. Verifies that the current version in app/build.gradle.kts is documented in RELEASE_NOTES.md.
2. Verifies that WhatsNewRepository.kt has a dedicated release branch for the current version.
3. Verifies that all string resources used in the current version's release notes exist
   and are non-empty across all 13 supported language directories.
"""

import os
import re
import sys

# Ensure UTF-8 output
if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

SUPPORTED_LOCALES = [
    "values",
    "values-bn",
    "values-de",
    "values-es",
    "values-fr",
    "values-gu",
    "values-hi",
    "values-mr",
    "values-pt",
    "values-pt-rBR",
    "values-ru",
    "values-ta",
    "values-te"
]


def extract_gradle_version():
    gradle_file = os.path.join("app", "build.gradle.kts")
    if not os.path.exists(gradle_file):
        raise FileNotFoundError(f"Missing {gradle_file}")

    with open(gradle_file, "r", encoding="utf-8") as f:
        content = f.read()

    name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    code_match = re.search(r'versionCode\s*=\s*(\d+)', content)

    if not name_match:
        raise ValueError("Could not find versionName in build.gradle.kts")
    if not code_match:
        raise ValueError("Could not find versionCode in build.gradle.kts")

    version_name = name_match.group(1).strip()
    version_code = int(code_match.group(1).strip())
    patch_num = int(version_name.split(".")[-1])

    return version_name, version_code, patch_num


def check_release_notes(version_name):
    notes_file = "RELEASE_NOTES.md"
    if not os.path.exists(notes_file):
        return False, f"Missing {notes_file}"

    with open(notes_file, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = rf"##\s+.*?(?:v)?{re.escape(version_name)}\b"
    if not re.search(pattern, content, re.IGNORECASE):
        return False, f"RELEASE_NOTES.md does not contain a section for v{version_name}"

    return True, "RELEASE_NOTES.md contains matching release section"


def check_whats_new_repository(patch_num):
    repo_file = os.path.join("app", "src", "main", "java", "com", "hrshd1eux", "imava", "core", "util", "WhatsNewRepository.kt")
    if not os.path.exists(repo_file):
        return False, [], f"Missing {repo_file}"

    with open(repo_file, "r", encoding="utf-8") as f:
        content = f.read()

    # Find when branches inside getItemsForVersion
    matches = list(re.finditer(r'((?:patch\b[^\n]+?|else))\s*->\s*listOf\(', content))
    if not matches:
        return False, [], "Could not parse getItemsForVersion in WhatsNewRepository.kt"

    matching_items_block = None

    for i, m in enumerate(matches):
        cond = m.group(1).strip()
        matched = False

        gte_match = re.search(r'patch\s*>=\s*(\d+)', cond)
        if gte_match and patch_num >= int(gte_match.group(1)):
            matched = True

        eq_match = re.search(r'patch\s*==\s*(\d+)', cond)
        if eq_match and patch_num == int(eq_match.group(1)):
            matched = True

        range_match = re.search(r'patch\s+in\s+(\d+)\.\.(\d+)', cond)
        if range_match and int(range_match.group(1)) <= patch_num <= int(range_match.group(2)):
            matched = True

        if matched:
            start = m.end()
            end = matches[i + 1].start() if i + 1 < len(matches) else content.find('}\n}', start)
            matching_items_block = content[start:end]
            break

    if not matching_items_block:
        return False, [], f"No matching release branch in getItemsForVersion for patch {patch_num}"

    # Extract all R.string.<key> references in this branch
    string_keys = re.findall(r'R\.string\.(\w+)', matching_items_block)
    if len(string_keys) < 4:
        return False, string_keys, f"Matching branch for patch {patch_num} only has {len(string_keys)} strings (expected at least 4 items = 8 string keys)"

    return True, string_keys, f"Found {len(string_keys)} string keys for patch {patch_num}"


def check_string_localizations(string_keys):
    base_res = os.path.join("app", "src", "main", "res")
    missing = {}

    for locale in SUPPORTED_LOCALES:
        file_path = os.path.join(base_res, locale, "strings.xml")
        if not os.path.exists(file_path):
            missing[locale] = ["FILE_MISSING"]
            continue

        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        locale_missing = []
        for key in string_keys:
            tag_pattern = rf'<string\s+name="{re.escape(key)}">([^<]*)</string>'
            match = re.search(tag_pattern, content)
            if not match or not match.group(1).strip():
                locale_missing.append(f"{key} (MISSING/EMPTY)")
            else:
                val = match.group(1)
                # Check for unescaped apostrophe in Android XML
                unescaped = re.findall(r'(?<!\\)\'', val)
                if unescaped:
                    locale_missing.append(f"{key} (UNESCAPED_APOSTROPHE)")

        if locale_missing:
            missing[locale] = locale_missing

    return missing


def main():
    print("🔍 [What's New Verifier] Running GEMINI.md Rule 6 Pre-Commit Audit...")
    try:
        version_name, version_code, patch_num = extract_gradle_version()
        print(f"📦 Active Target: Imava v{version_name} (code: {version_code}, patch: {patch_num})")
    except Exception as e:
        print(f"❌ Error extracting gradle version: {e}")
        sys.exit(1)

    # 1. Check RELEASE_NOTES.md
    ok_rn, msg_rn = check_release_notes(version_name)
    if not ok_rn:
        print(f"❌ {msg_rn}")
        print("👉 Action: Add a section for this version in RELEASE_NOTES.md before committing.")
        sys.exit(1)
    print(f"✅ {msg_rn}")

    # 2. Check WhatsNewRepository.kt
    ok_repo, string_keys, msg_repo = check_whats_new_repository(patch_num)
    if not ok_repo:
        print(f"❌ {msg_repo}")
        print(f"👉 Action: Add a dedicated branch for patch {patch_num} in WhatsNewRepository.getItemsForVersion().")
        sys.exit(1)
    print(f"✅ WhatsNewRepository.kt has valid entries ({len(string_keys)} string keys configured)")

    # 3. Check 13 Locales
    missing = check_string_localizations(string_keys)
    if missing:
        print("❌ Missing localized string resources:")
        for loc, keys in missing.items():
            print(f"   • {loc}: {', '.join(keys)}")
        print("👉 Action: Translate and add all missing string keys across all 13 supported languages.")
        sys.exit(1)

    print(f"✅ All {len(string_keys)} What's New string resources verified across all 13 languages!")
    print(f"🎉 Verification SUCCESSFUL: What's New is 100% up to date for v{version_name}!")
    sys.exit(0)


if __name__ == "__main__":
    main()
