#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if [[ $# -lt 2 ]]; then
  echo 'Usage: bash scripts/glyphora-commit.sh "type: message" fichier1 fichier2 ...'
  exit 2
fi

message="$1"
shift
files=("$@")

if [[ -z "$message" ]]; then
  echo "ERREUR: le message de commit est vide."
  exit 2
fi

if ! git diff --cached --quiet; then
  echo "ERREUR: des changements sont deja indexes."
  echo "Examine-les et valide-les ou desindexe-les manuellement."
  exit 1
fi

echo "=== BRANCHE ==="
git branch --show-current

echo
echo "=== FICHIERS SELECTIONNES ==="
for file in "${files[@]}"; do
  if [[ -d "$file" ]]; then
    echo "ERREUR: indique des fichiers, pas un dossier: $file"
    exit 2
  fi

  if [[ ! -e "$file" ]] && ! git ls-files --error-unmatch -- "$file" >/dev/null 2>&1; then
    echo "ERREUR: fichier inconnu: $file"
    exit 2
  fi

  printf ' - %s\n' "$file"
done

echo
echo "=== ETAT ACTUEL ==="
git status --short

echo
read -r -p "Indexer uniquement ces fichiers ? [y/N] " answer
case "$answer" in
  y|Y|yes|YES) ;;
  *) echo "Annule: aucun changement indexe par ce script."; exit 0 ;;
esac

git add -- "${files[@]}"

echo
echo "=== DIFF COMPLET DU COMMIT ==="
git diff --cached --check
git diff --cached --stat
git diff --cached

echo
printf 'Message: %s\n' "$message"
read -r -p "Confirmer le commit local ? [y/N] " answer
case "$answer" in
  y|Y|yes|YES) ;;
  *)
    git reset -q -- "${files[@]}"
    echo "Commit annule; les changements selectionnes restent dans le repertoire de travail."
    exit 0
    ;;
esac

git commit -m "$message"

echo
echo "=== COMMIT CREE ==="
git log -1 --oneline
echo "Aucun push ni merge effectue."
