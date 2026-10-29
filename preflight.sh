#!/bin/bash
# ==============================================================================
# APENKODER SOFTWARE - GOOGLE PLAY PRE-FLIGHT COMPLIANCE AUDIT (v1.1)
# Run from the project root via `bash preflight.sh` in Git Bash or WSL.
# ==============================================================================

export CLAUDE_TARGET_API=36
export LOG_DIR="./debug_logs"
mkdir -p "$LOG_DIR"
FAILURES=0

echo "=== [1/8] RELEASE LINT (lintVitalRelease) ==="
if ./gradlew lintVitalRelease --stacktrace > "$LOG_DIR/lint_vital_results.log" 2>&1; then
    echo "[OK] lintVitalRelease passed."
else
    echo "[FAIL] lintVitalRelease errors. Review $LOG_DIR/lint_vital_results.log"
    FAILURES=$((FAILURES + 1))
fi

echo "=== [2/8] TARGET API LEVEL (required >= $CLAUDE_TARGET_API) ==="
TARGET_LINE=$(grep -hE '^\s*targetSdk\b' app/build.gradle.kts app/build.gradle 2>/dev/null | head -n1)
echo "Found: $TARGET_LINE"
TARGET_VALUE=$(echo "$TARGET_LINE" | grep -oE '\b[0-9]+\b' | head -n1)
if [ -z "$TARGET_VALUE" ]; then
    # Version-catalog reference, e.g. libs.versions.targetSdk.get().toInt()
    KEY=$(echo "$TARGET_LINE" | grep -oE 'libs\.versions\.[A-Za-z0-9_.]+' | sed -E 's/^libs\.versions\.//; s/\.get$//')
    KEY_RE=$(echo "$KEY" | sed 's/\./[-_.]/g')
    TARGET_VALUE=$(grep -E "^\s*${KEY_RE}\s*=" gradle/libs.versions.toml 2>/dev/null | grep -oE '[0-9]+' | head -n1)
    echo "Resolved catalog key '$KEY' to: ${TARGET_VALUE:-not found}"
fi
if [ -n "$TARGET_VALUE" ] && [ "$TARGET_VALUE" -ge "$CLAUDE_TARGET_API" ]; then
    echo "[OK] targetSdk $TARGET_VALUE meets the API $CLAUDE_TARGET_API requirement."
else
    echo "[FAIL] targetSdk '${TARGET_VALUE:-unresolved}' is below $CLAUDE_TARGET_API or could not be resolved."
    FAILURES=$((FAILURES + 1))
fi

echo "=== [3/8] CLEARTEXT TRAFFIC ==="
grep -rn "http://" app/src/main/ 2>/dev/null | grep -v "schemas.android.com" | grep -v "xmlns" > "$LOG_DIR/cleartext_leaks.log"
CLEARTEXT_COUNT=$(wc -l < "$LOG_DIR/cleartext_leaks.log")
if [ "$CLEARTEXT_COUNT" -gt 0 ]; then
    echo "[WARN] $CLEARTEXT_COUNT http:// references. Review $LOG_DIR/cleartext_leaks.log"
else
    echo "[OK] No raw cleartext URLs in main sources."
fi
if grep -q 'usesCleartextTraffic="true"' app/src/main/AndroidManifest.xml 2>/dev/null; then
    echo "[FAIL] usesCleartextTraffic=\"true\" is enabled in the manifest."
    FAILURES=$((FAILURES + 1))
else
    echo "[OK] Manifest cleartext traffic is restricted."
fi

echo "=== [4/8] DEVICE IDENTIFIER CALLS ==="
grep -rnE "getDeviceId|advertisingId|getMacAddress|ANDROID_ID" app/src/main/ 2>/dev/null > "$LOG_DIR/tracked_identifiers.log"
IDENTIFIER_COUNT=$(wc -l < "$LOG_DIR/tracked_identifiers.log")
if [ "$IDENTIFIER_COUNT" -gt 0 ]; then
    echo "[WARN] $IDENTIFIER_COUNT identifier references. Match Play Console Data Safety. Review $LOG_DIR/tracked_identifiers.log"
else
    echo "[OK] No direct tracking identifiers in main sources."
fi

echo "=== [5/8] DEBUG DEPENDENCY ISOLATION ==="
grep -hiE '^\s*implementation\(.*(leak|flipper|chucker)' app/build.gradle.kts app/build.gradle 2>/dev/null > "$LOG_DIR/leaked_debug_deps.log"
LEAK_COUNT=$(wc -l < "$LOG_DIR/leaked_debug_deps.log")
if [ "$LEAK_COUNT" -gt 0 ]; then
    echo "[FAIL] Debug tools on 'implementation' instead of 'debugImplementation'. Review $LOG_DIR/leaked_debug_deps.log"
    FAILURES=$((FAILURES + 1))
else
    echo "[OK] Debug tools isolated from the release variant."
fi

echo "=== [6/8] HARDCODED SECRETS AND COMMITTED KEY FILES (Section 11.1) ==="
grep -rniE "password|secret|api_?key|token|Bearer |-----BEGIN" app/src/main/ gradle.properties 2>/dev/null > "$LOG_DIR/possible_secrets.log"
SECRET_COUNT=$(wc -l < "$LOG_DIR/possible_secrets.log")
if [ "$SECRET_COUNT" -gt 0 ]; then
    echo "[WARN] $SECRET_COUNT possible secret references (expect noise from UI labels). Review $LOG_DIR/possible_secrets.log"
else
    echo "[OK] No secret-like strings in main sources."
fi
git ls-files 2>/dev/null | grep -iE '\.(pem|key|jks|p12|keystore)$|(^|/)\.env' | grep -v '^debug\.keystore$' > "$LOG_DIR/committed_key_files.log"
KEYFILE_COUNT=$(wc -l < "$LOG_DIR/committed_key_files.log")
if [ "$KEYFILE_COUNT" -gt 0 ]; then
    echo "[FAIL] $KEYFILE_COUNT key or .env files tracked in git (debug.keystore excluded). Review $LOG_DIR/committed_key_files.log"
    FAILURES=$((FAILURES + 1))
else
    echo "[OK] No key or .env files tracked in git beyond debug.keystore."
fi

echo "=== [7/8] RELEASE MANIFEST FLAGS: DEBUGGABLE AND BACKUP (Section 11.1, 11.4) ==="
if grep -q 'android:debuggable="true"' app/src/main/AndroidManifest.xml 2>/dev/null; then
    echo "[FAIL] android:debuggable=\"true\" is hardcoded in the main manifest."
    FAILURES=$((FAILURES + 1))
else
    echo "[OK] No hardcoded debuggable flag in the main manifest."
fi
if grep -q 'android:allowBackup="true"' app/src/main/AndroidManifest.xml 2>/dev/null; then
    echo "[WARN] allowBackup is on. Confirm dataExtractionRules / fullBackupContent exclude tokens and session data (11.4)."
else
    echo "[OK] allowBackup is off or unset; confirm the intended behaviour in 11.4."
fi

echo "=== [8/8] RELEASE UNIT TESTS ==="
if ./gradlew testReleaseUnitTest > "$LOG_DIR/unit_test_results.log" 2>&1; then
    echo "[OK] Release unit tests passed."
else
    echo "[FAIL] Release unit tests broken. Review $LOG_DIR/unit_test_results.log"
    FAILURES=$((FAILURES + 1))
fi

echo "=============================================================================="
echo "Pre-flight complete: $FAILURES failing check(s). Logs in $LOG_DIR/."
echo "=============================================================================="
[ "$FAILURES" -eq 0 ]
