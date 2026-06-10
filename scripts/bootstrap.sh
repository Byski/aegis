#!/usr/bin/env bash
#
# One-time local setup. Installs the toolchain this project needs and wires the
# git hooks path. Safe to re-run; each step is idempotent.
#
# Tested on macOS (Apple Silicon) with Homebrew.
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

echo "==> Wiring git hooks"
git config core.hooksPath .githooks
chmod +x .githooks/* scripts/*.sh 2>/dev/null || true

if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew not found. Install it from https://brew.sh then re-run." >&2
  exit 1
fi

echo "==> Installing CLI tooling via Homebrew"
BREW_PKGS=(kind helm helmfile k6 trivy terraform kubectl)
for pkg in "${BREW_PKGS[@]}"; do
  if brew list --formula "$pkg" >/dev/null 2>&1; then
    echo "    $pkg already installed"
  else
    brew install "$pkg"
  fi
done

echo "==> Installing tflocal (Terraform wrapper for LocalStack)"
if ! command -v tflocal >/dev/null 2>&1; then
  if command -v pipx >/dev/null 2>&1; then
    pipx install terraform-local
  else
    python3 -m pip install --user terraform-local
  fi
fi

echo "==> Java 21 (via SDKMAN if present)"
if [ -s "${SDKMAN_DIR:-$HOME/.sdkman}/bin/sdkman-init.sh" ]; then
  # shellcheck disable=SC1090
  source "${SDKMAN_DIR:-$HOME/.sdkman}/bin/sdkman-init.sh"
  sdk install java 21.0.5-tem || true
  echo "    Run: sdk use java 21.0.5-tem   (in this repo, per shell)"
else
  echo "    SDKMAN not found. Install from https://sdkman.io then run:"
  echo "    sdk install java 21.0.5-tem"
fi

echo "==> Bootstrap complete"
