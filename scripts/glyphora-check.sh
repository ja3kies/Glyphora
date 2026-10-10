#!/usr/bin/env bash
set -u

cd "$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "ERREUR: pas dans un depot Git."
  exit 1
}

section() { printf '\n=== %s ===\n' "$1"; }

section "BRANCHE ET SUIVI"
git status --short --branch
git branch -vv

branch="$(git branch --show-current)"
upstream="$(git rev-parse --abbrev-ref \
  --symbolic-full-name '@{upstream}' 2>/dev/null || true)"

if [[ -n "$upstream" ]]; then
  printf '\nBranche distante suivie: %s\n' "$upstream"
  git rev-list --left-right --count "HEAD...$upstream"
else
  printf '\nATTENTION: aucun upstream configure pour %s.\n' "$branch"
  if git show-ref --verify --quiet "refs/remotes/origin/$branch"; then
    printf 'Branche distante existante: origin/%s\n' "$branch"
    printf 'Aucune modification automatique effectuee.\n'
  fi
fi

section "DIFFERENCES NON INDEXEES"
git diff --stat
git diff --check

section "DIFFERENCES INDEXEES"
git diff --cached --stat
git diff --cached --check

section "FICHIERS NON SUIVIS"
git ls-files --others --exclude-standard

section "DERNIERS COMMITS"
git log --oneline --decorate -8

section "OUTILS"
for tool in git gh java bash; do
  if command -v "$tool" >/dev/null 2>&1; then
    printf '%s: %s\n' "$tool" "$(command -v "$tool")"
  else
    printf '%s: indisponible\n' "$tool"
  fi
done

section "FIN DU DIAGNOSTIC"
