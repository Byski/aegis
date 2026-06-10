#!/usr/bin/env bash
#
# Repository hygiene gate.
#
# Enforces the project's professional-code standards on staged changes (or the
# whole tree in CI). It rejects:
#   1. References to disallowed vendors, products, or assistant terms.
#   2. Em-dash characters in prose or comments.
#   3. Emoji in source, docs, or configuration.
#   4. Accidentally committed secrets (private keys, cloud access keys, dotenv files).
#
# The disallowed-term patterns below are written as character classes on purpose,
# so this file contains none of the literal words it screens for and therefore
# passes its own check.
#
# Usage:
#   scripts/check-hygiene.sh --staged   # check files staged for commit (pre-commit hook)
#   scripts/check-hygiene.sh --all      # check every tracked file (CI)
set -euo pipefail

MODE="${1:---staged}"
REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

python3 - "$MODE" <<'PY'
import re
import subprocess
import sys

mode = sys.argv[1] if len(sys.argv) > 1 else "--staged"

# Paths we never scan: vendored deps, build output, binaries, the lockfiles.
SKIP_PREFIXES = (
    ".git/", "node_modules/", "target/", "build/", ".terraform/",
    "dist/", "venv/", ".venv/", "__pycache__/",
)
SKIP_SUFFIXES = (
    ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg", ".pdf", ".webp",
    ".jar", ".class", ".zip", ".gz", ".tar", ".duckdb", ".db", ".lock",
    ".woff", ".woff2", ".ttf", ".mp4", ".mov",
)

# Disallowed vendor / product / assistant terms, spelled as character classes
# so the literal words do not appear in this file.
NAME_PATTERNS = [
    r"[Cc][Ll][Aa][Uu][Dd][Ee]",
    r"[Aa][Nn][Tt][Hh][Rr][Oo][Pp][Ii][Cc]",
    r"[Ss][Oo][Nn][Nn][Ee][Tt]",
    r"\b[Oo][Pp][Uu][Ss]\b",
    r"\b[Hh][Aa][Ii][Kk][Uu]\b",
    r"\b[Ff][Aa][Bb][Ll][Ee]\b",
    r"[Cc][Oo][Pp][Ii][Ll][Oo][Tt]",
    r"[Oo][Pp][Ee][Nn][Aa][Ii]",
    r"\b[A][I]\b",                       # standalone assistant abbreviation
    r"[A][I][-_ ]?[Gg]enerated",
]

# Secret indicators.
SECRET_PATTERNS = [
    r"-----BEGIN [A-Z ]*PRIVATE KEY-----",
    r"\bAKIA[0-9A-Z]{16}\b",
    r"\bASIA[0-9A-Z]{16}\b",
    r"(?i)\b(secret|password|passwd|token|api[_-]?key)\b\s*[:=]\s*['\"][^'\"]{8,}['\"]",
]

EM_DASH = chr(0x2014)

# Emoji codepoint ranges.
EMOJI_RANGES = [
    (0x1F300, 0x1F5FF), (0x1F600, 0x1F64F), (0x1F680, 0x1F6FF),
    (0x1F900, 0x1F9FF), (0x1FA70, 0x1FAFF), (0x2600, 0x26FF),
    (0x2700, 0x27BF), (0x1F1E6, 0x1F1FF), (0x2B00, 0x2BFF),
]
VARIATION_SELECTOR = 0xFE0F


def is_emoji(ch):
    cp = ord(ch)
    if cp == VARIATION_SELECTOR:
        return True
    return any(lo <= cp <= hi for lo, hi in EMOJI_RANGES)


def staged_files():
    out = subprocess.run(
        ["git", "diff", "--cached", "--name-only", "--diff-filter=ACMR"],
        capture_output=True, text=True, check=True,
    ).stdout
    return [f for f in out.splitlines() if f.strip()]


def tracked_files():
    out = subprocess.run(
        ["git", "ls-files"], capture_output=True, text=True, check=True,
    ).stdout
    return [f for f in out.splitlines() if f.strip()]


def read_staged(path):
    res = subprocess.run(["git", "show", f":{path}"], capture_output=True)
    if res.returncode != 0:
        return None
    return res.stdout


def read_tracked(path):
    try:
        with open(path, "rb") as fh:
            return fh.read()
    except OSError:
        return None


def should_skip(path):
    if path.startswith(SKIP_PREFIXES):
        return True
    if path.endswith(SKIP_SUFFIXES):
        return True
    return False


def check_dotenv(path):
    name = path.rsplit("/", 1)[-1]
    if name == ".env" or (name.startswith(".env.") and not name.endswith(".example")):
        return [f"{path}: dotenv file must not be committed (use .env.example)"]
    return []


def scan(path, raw):
    findings = []
    findings += check_dotenv(path)
    if raw is None:
        return findings
    if b"\x00" in raw[:4096]:          # binary
        return findings
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        return findings

    for lineno, line in enumerate(text.splitlines(), start=1):
        for pat in NAME_PATTERNS:
            m = re.search(pat, line)
            if m:
                findings.append(f"{path}:{lineno}: disallowed term match")
                break
        if EM_DASH in line:
            findings.append(f"{path}:{lineno}: em-dash character not allowed")
        for ch in line:
            if is_emoji(ch):
                findings.append(f"{path}:{lineno}: emoji not allowed")
                break
        for pat in SECRET_PATTERNS:
            if re.search(pat, line):
                findings.append(f"{path}:{lineno}: possible secret committed")
                break
    return findings


if mode == "--staged":
    files, reader = staged_files(), read_staged
elif mode == "--all":
    files, reader = tracked_files(), read_tracked
else:
    print(f"unknown mode: {mode}", file=sys.stderr)
    sys.exit(2)

all_findings = []
for path in files:
    if should_skip(path):
        continue
    all_findings += scan(path, reader(path))

if all_findings:
    print("Hygiene check failed:")
    for f in all_findings:
        print(f"  {f}")
    print("\nFix the lines above. See CONTRIBUTING.md for the standard.")
    sys.exit(1)

print("Hygiene check passed.")
PY
