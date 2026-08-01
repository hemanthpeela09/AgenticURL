#!/usr/bin/env sh
set -e

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "Gradle is not installed or not on PATH. Install Gradle or add it to PATH, then rerun this command." >&2
  exit 1
fi
