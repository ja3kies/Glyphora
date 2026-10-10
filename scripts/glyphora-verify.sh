#!/usr/bin/env bash
set -u

cd "$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "ERREUR: pas dans un depot Git."
  exit 1
}

echo "=== ENVIRONNEMENT ==="
java -version 2>&1 || true

sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

if [[ -z "$sdk" ]]; then
  for candidate in "$HOME/Android/Sdk" "$HOME/android-sdk"; do
    if [[ -d "$candidate" ]]; then
      sdk="$candidate"
      break
    fi
  done
fi

if [[ -n "$sdk" && -d "$sdk" ]]; then
  export ANDROID_HOME="${ANDROID_HOME:-$sdk}"
  export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$sdk}"
  echo "SDK Android detecte: $sdk"
else
  echo "SDK Android non detecte."
  echo "Compilation locale ignoree: environnement Android incomplet."
  echo
  echo "=== DERNIERES EXECUTIONS GITHUB ACTIONS ==="
  if command -v gh >/dev/null 2>&1; then
    gh run list --repo ja3kies/Glyphora --limit 5 || true
  else
    echo "GitHub CLI indisponible."
  fi
  echo
  echo "Aucun test ni build local n'a ete lance."
  exit 0
fi

if [[ ! -f gradle/wrapper/gradle-wrapper.jar ]]; then
  echo "Wrapper Gradle incomplet: gradle-wrapper.jar absent."
  echo "Compilation locale non lancee."
  exit 2
fi

if [[ ! -f local.properties ]]; then
  echo "INFO: local.properties absent; le build peut necessiter une configuration SDK."
fi

echo
echo "=== TESTS ET COMPILATION DEBUG ==="
if bash ./gradlew testDebugUnitTest assembleDebug; then
  echo
  echo "SUCCES: tests et compilation termines."
  echo "APK eventuel: app/build/outputs/apk/debug/"
else
  result=$?
  echo
  echo "ECHEC: Gradle a retourne le code $result."
  echo "Conserver la sortie complete pour identifier la cause."
  exit "$result"
fi
